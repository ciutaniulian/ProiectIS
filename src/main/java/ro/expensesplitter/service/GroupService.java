package ro.expensesplitter.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ro.expensesplitter.dto.GroupActivityResponse;
import ro.expensesplitter.dto.GroupDetailsResponse;
import ro.expensesplitter.dto.GroupInvitationResponse;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.entity.ExpenseGroup;
import ro.expensesplitter.entity.GroupActivity;
import ro.expensesplitter.entity.GroupInvitation;
import ro.expensesplitter.entity.GroupMembership;
import ro.expensesplitter.entity.GroupStatus;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.ExpenseGroupRepository;
import ro.expensesplitter.repository.GroupActivityRepository;
import ro.expensesplitter.repository.GroupInvitationRepository;
import ro.expensesplitter.repository.GroupMembershipRepository;
import ro.expensesplitter.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static ro.expensesplitter.entity.GroupInvitation.Status.*;

@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class GroupService {
    private static final Duration INVITATION_LIFETIME = Duration.ofDays(7);

    private final ExpenseGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final GroupInvitationRepository invitations;
    private final GroupActivityRepository activities;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final SecureRandom tokenRandom = new SecureRandom();

    public GroupService(ExpenseGroupRepository groups, GroupMembershipRepository memberships,
                        GroupInvitationRepository invitations, GroupActivityRepository activities,
                        UserRepository users, ApplicationEventPublisher events) {
        this.groups = groups;
        this.memberships = memberships;
        this.invitations = invitations;
        this.activities = activities;
        this.users = users;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> list(String email) {
        return groups.findForMember(email);
    }

    public GroupResponse create(String name, String description, String email) {
        var creator = users.findByEmail(email).orElseThrow(() ->
                new GroupOperationException(HttpStatus.UNAUTHORIZED, "ACCOUNT_NOT_FOUND", "Sign in to create a group."));
        var group = groups.save(new ExpenseGroup(name.strip(), cleanDescription(description), creator.getId()));
        memberships.save(new GroupMembership(group.getId(), creator));
        record(group, GroupActivity.Type.CREATED, creator, null);
        return summary(group, 1);
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse details(Long id, String email) {
        var group = groups.findById(id).orElseThrow(GroupService::forbidden);
        var members = memberships.findMembers(id);
        requireMember(members, email);
        return details(group, members);
    }

    public GroupDetailsResponse update(Long id, String name, String description, String email) {
        var group = lockForMember(id, email);
        requireActive(group);
        var members = memberships.findMembers(id);
        var actor = requireMember(members, email);
        String cleanName = name.strip();
        String cleanDescription = cleanDescription(description);
        if (!group.getName().equals(cleanName) || !Objects.equals(group.getDescription(), cleanDescription)) {
            group.updateDetails(cleanName, cleanDescription);
            record(group, GroupActivity.Type.UPDATED, actor.getUser(), null);
        }
        return details(group, members);
    }

    public GroupDetailsResponse removeMember(Long id, Long userId, String email) {
        var group = lockForMember(id, email);
        requireActive(group);
        var members = memberships.findMembers(id);
        var actor = requireMember(members, email);
        if (actor.getUser().getId().equals(userId)) {
            throw conflict("USE_LEAVE_GROUP", "Use Leave group to remove your own membership.");
        }
        var target = members.stream().filter(member -> member.getUser().getId().equals(userId)).findFirst()
                .orElseThrow(() -> new GroupOperationException(
                        HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "That person is no longer a member."));
        remove(group, target);
        record(group, GroupActivity.Type.REMOVED, actor.getUser(), target.getUser());
        return details(group, memberships.findMembers(id));
    }

    public void leave(Long id, boolean confirmClose, String email) {
        var group = lockForMember(id, email);
        var members = memberships.findMembers(id);
        var actor = requireMember(members, email);
        boolean lastMember = members.size() == 1;
        if (lastMember && !confirmClose) {
            throw conflict("LAST_MEMBER_CONFIRMATION_REQUIRED", "Leaving now closes this group.");
        }
        remove(group, actor);
        record(group, GroupActivity.Type.LEFT, actor.getUser(), null);
        if (lastMember) {
            group.changeStatus(GroupStatus.CLOSED);
            pendingInvitations(id, Instant.now()).forEach(invitation -> invitation.changeStatus(REVOKED));
            record(group, GroupActivity.Type.CLOSED, actor.getUser(), null);
        }
    }

    public GroupDetailsResponse archive(Long id, String email) {
        return changeGroupStatus(id, email, GroupStatus.ARCHIVED);
    }

    public GroupDetailsResponse restore(Long id, String email) {
        return changeGroupStatus(id, email, GroupStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public GroupActivityResponse activity(Long id, String email, int page, int size) {
        if (!memberships.existsByGroupIdAndUserEmail(id, email)) {
            throw forbidden();
        }
        var result = activities.findByGroupIdOrderByIdDesc(id, PageRequest.of(page, size));
        return new GroupActivityResponse(result.map(event -> new GroupActivityResponse.Item(event.getId(),
                event.getType(), event.getActorName(), event.getSubjectName(), event.getCreatedAt())).getContent(),
                result.hasNext());
    }

    public GroupInvitationResponse.Created invite(Long id, String memberEmail, String email) {
        var group = lockForMember(id, email);
        requireActive(group);
        String recipient = memberEmail.strip().toLowerCase(Locale.ROOT);
        if (memberships.existsByGroupIdAndUserEmail(id, recipient)) {
            throw conflict("MEMBER_ALREADY_JOINED", "That person already belongs to this group.");
        }
        Instant now = Instant.now();
        if (invitations.existsByGroupIdAndEmailAndStatusAndExpiresAtAfter(id, recipient, PENDING, now)) {
            throw conflict("INVITATION_ALREADY_PENDING", "That email address already has a pending invitation.");
        }
        String token = createToken();
        var invitation = invitations.save(new GroupInvitation(group, recipient, hash(token), now.plus(INVITATION_LIFETIME)));
        return new GroupInvitationResponse.Created(invitation.getId(), recipient, PENDING, invitation.getExpiresAt(), token);
    }

    public GroupDetailsResponse revoke(Long id, Long invitationId, String email) {
        var group = lockForMember(id, email);
        requireActive(group);
        var invitation = invitations.findByIdAndGroupId(invitationId, id).orElseThrow(GroupService::invitationNotFound);
        requirePending(invitation);
        invitation.changeStatus(REVOKED);
        return details(group, memberships.findMembers(id));
    }

    @Transactional(readOnly = true)
    public List<GroupInvitationResponse.Received> inbox(String email) {
        Instant now = Instant.now();
        return invitations.findInbox(email, PENDING, now).stream().map(invitation -> received(invitation, now)).toList();
    }

    @Transactional(readOnly = true)
    public GroupInvitationResponse.Received invitationByLink(String token, String email) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw invitationNotFound();
        }
        var invitation = invitations.findForRecipientByToken(hash(token), email)
                .orElseThrow(GroupService::invitationNotFound);
        Instant now = Instant.now();
        if (invitation.statusAt(now) == PENDING && invitation.getGroup().getStatus() == GroupStatus.ARCHIVED) {
            throw conflict("GROUP_NOT_ACTIVE", "This invitation is paused while the group is archived.");
        }
        return received(invitation, now);
    }

    public GroupResponse accept(Long invitationId, String email) {
        var invitation = lockRecipientInvitation(invitationId, email);
        var group = invitation.getGroup();
        var user = users.findByEmail(email).orElseThrow(GroupService::invitationNotFound);
        if (memberships.existsByGroupIdAndUserEmail(group.getId(), email)) {
            throw conflict("MEMBER_ALREADY_JOINED", "You already belong to this group.");
        }
        memberships.save(new GroupMembership(group.getId(), user));
        invitation.changeStatus(ACCEPTED);
        record(group, GroupActivity.Type.JOINED, user, null);
        return summary(group, memberships.countByGroupId(group.getId()));
    }

    public void decline(Long invitationId, String email) {
        lockRecipientInvitation(invitationId, email).changeStatus(DECLINED);
    }

    private ExpenseGroup lockForMember(Long id, String email) {
        if (!memberships.existsByGroupIdAndUserEmail(id, email)) {
            throw forbidden();
        }
        var group = groups.findForUpdate(id).orElseThrow(GroupService::forbidden);
        if (!memberships.existsByGroupIdAndUserEmail(id, email)) {
            throw forbidden();
        }
        return group;
    }

    private GroupInvitation lockRecipientInvitation(Long invitationId, String email) {
        Long id = invitations.findGroupIdForRecipient(invitationId, email).orElseThrow(GroupService::invitationNotFound);
        var group = groups.findForUpdate(id).orElseThrow(GroupService::invitationNotFound);
        var invitation = invitations.findByIdAndGroupId(invitationId, id).orElseThrow(GroupService::invitationNotFound);
        requirePending(invitation);
        requireActive(group);
        return invitation;
    }

    private GroupDetailsResponse changeGroupStatus(Long id, String email, GroupStatus status) {
        var group = lockForMember(id, email);
        var members = memberships.findMembers(id);
        var actor = requireMember(members, email).getUser();
        if (status == GroupStatus.ARCHIVED) {
            requireActive(group);
            record(group, GroupActivity.Type.ARCHIVED, actor, null);
        } else {
            if (group.getStatus() != GroupStatus.ARCHIVED) {
                throw conflict("GROUP_NOT_ARCHIVED", "Only an archived group can be restored.");
            }
            record(group, GroupActivity.Type.RESTORED, actor, null);
        }
        group.changeStatus(status);
        events.publishEvent(new GroupStatusChanged(id, status));
        return details(group, members);
    }

    private void remove(ExpenseGroup group, GroupMembership target) {
        memberships.delete(target);
        events.publishEvent(new GroupAccessChanged(group.getId(), target.getUser().getEmail()));
    }

    private void record(ExpenseGroup group, GroupActivity.Type type, User actor, User subject) {
        activities.save(new GroupActivity(group.getId(), type, actor, subject, Instant.now()));
    }

    private GroupDetailsResponse details(ExpenseGroup group, List<GroupMembership> members) {
        Instant now = Instant.now();
        return new GroupDetailsResponse(summary(group, members.size()), members.stream()
                .map(GroupMembership::getUser)
                .map(user -> new GroupDetailsResponse.Member(user.getId(), user.getName()))
                .toList(), pendingInvitations(group.getId(), now).stream()
                .map(invitation -> new GroupDetailsResponse.Invitation(invitation.getId(), invitation.getEmail(),
                        PENDING, invitation.getExpiresAt()))
                .toList());
    }

    private List<GroupInvitation> pendingInvitations(Long groupId, Instant now) {
        return invitations.findByGroupIdAndStatusAndExpiresAtAfterOrderByIdDesc(groupId, PENDING, now);
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        tokenRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static GroupInvitationResponse.Received received(GroupInvitation invitation, Instant now) {
        var status = invitation.statusAt(now);
        if (status == PENDING) {
            var group = invitation.getGroup();
            return new GroupInvitationResponse.Received(invitation.getId(), group.getId(), group.getName(),
                    invitation.getExpiresAt(), status);
        }
        return new GroupInvitationResponse.Received(invitation.getId(), null, null, invitation.getExpiresAt(), status);
    }

    private static GroupResponse summary(ExpenseGroup group, long memberCount) {
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(), memberCount, group.getStatus());
    }

    private static GroupMembership requireMember(List<GroupMembership> members, String email) {
        return members.stream().filter(member -> member.getUser().getEmail().equals(email)).findFirst()
                .orElseThrow(GroupService::forbidden);
    }

    private static void requireActive(ExpenseGroup group) {
        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw conflict("GROUP_NOT_ACTIVE", "This group is not active.");
        }
    }

    private static void requirePending(GroupInvitation invitation) {
        var status = invitation.statusAt(Instant.now());
        if (status == EXPIRED) {
            throw conflict("INVITATION_EXPIRED", "This invitation has expired.");
        }
        if (status != PENDING) {
            throw conflict("INVITATION_NOT_PENDING", "This invitation is no longer pending.");
        }
    }

    private static GroupOperationException invitationNotFound() {
        return new GroupOperationException(HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND", "Invitation not found.");
    }

    private static GroupOperationException forbidden() {
        return new GroupOperationException(HttpStatus.FORBIDDEN, "GROUP_FORBIDDEN", "This group is unavailable to you.");
    }

    private static GroupOperationException conflict(String code, String message) {
        return new GroupOperationException(HttpStatus.CONFLICT, code, message);
    }

    private static String cleanDescription(String description) {
        return description == null || description.isBlank() ? null : description.strip();
    }
}
