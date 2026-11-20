package ro.expensesplitter.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ro.expensesplitter.dto.GroupActivityResponse;
import ro.expensesplitter.dto.GroupInvitationResponse;
import ro.expensesplitter.entity.ExpenseGroup;
import ro.expensesplitter.entity.GroupActivity;
import ro.expensesplitter.entity.GroupInvitation;
import ro.expensesplitter.entity.GroupStatus;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.ExpenseGroupRepository;
import ro.expensesplitter.repository.GroupInvitationRepository;
import ro.expensesplitter.repository.GroupMembershipRepository;
import ro.expensesplitter.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class GroupServiceIntegrationTest {
    @Autowired private GroupService service;
    @Autowired private ExpenseGroupRepository groups;
    @Autowired private GroupInvitationRepository invitations;
    @Autowired private GroupMembershipRepository memberships;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void membersHaveEqualPermissionsAndLifecycleChangesAreRecorded() {
        var creator = account("Creator");
        var guest = account("Guest");
        var outsider = account("Application Admin");
        jdbc.update("update users set role = 'ADMIN' where id = ?", outsider.getId());
        var group = service.create("Weekend", "  First description  ", creator.getEmail());
        assertThat(group.description()).isEqualTo("First description");
        assertThat(service.list(creator.getEmail())).containsExactly(group);
        assertThat(service.list(outsider.getEmail())).isEmpty();
        assertError("GROUP_FORBIDDEN", () -> service.details(group.id(), outsider.getEmail()));

        var invitation = service.invite(group.id(), guest.getEmail(), creator.getEmail());
        assertThat(service.list(guest.getEmail())).isEmpty();
        assertThat(service.accept(invitation.id(), guest.getEmail()).memberCount()).isEqualTo(2);

        var edited = service.update(group.id(), "New title", null, guest.getEmail());
        assertThat(edited.group().name()).isEqualTo("New title");
        assertThat(edited.group().description()).isNull();
        assertThat(service.archive(group.id(), guest.getEmail()).group().status()).isEqualTo(GroupStatus.ARCHIVED);
        assertThat(service.details(group.id(), creator.getEmail()).members()).hasSize(2);
        assertError("GROUP_NOT_ACTIVE", () -> service.update(group.id(), "Blocked", null, creator.getEmail()));
        assertError("GROUP_NOT_ACTIVE", () -> service.removeMember(group.id(), creator.getId(), guest.getEmail()));
        assertThat(service.restore(group.id(), creator.getEmail()).group().status()).isEqualTo(GroupStatus.ACTIVE);

        var afterRemoval = service.removeMember(group.id(), creator.getId(), guest.getEmail());
        assertThat(afterRemoval.group().memberCount()).isEqualTo(1);
        assertThat(service.list(creator.getEmail())).isEmpty();
        assertError("GROUP_FORBIDDEN", () -> service.activity(group.id(), creator.getEmail(), 0, 10));
        var events = service.activity(group.id(), guest.getEmail(), 0, 5);
        assertThat(events.hasNext()).isTrue();
        assertThat(events.items()).extracting(GroupActivityResponse.Item::type).containsExactly(
                GroupActivity.Type.REMOVED, GroupActivity.Type.RESTORED, GroupActivity.Type.ARCHIVED,
                GroupActivity.Type.UPDATED, GroupActivity.Type.JOINED);
        assertThat(events.items().get(0).subjectName()).isEqualTo("Creator");
        assertThat(service.activity(group.id(), guest.getEmail(), 1, 5).items())
                .extracting(GroupActivityResponse.Item::type).containsExactly(GroupActivity.Type.CREATED);
    }

    @Test
    void recipientControlsInvitationsEvenWhenRegisteringAfterDelivery() {
        var creator = account("Inviter");
        var outsider = account("Other Recipient");
        String recipient = uniqueEmail();
        var group = service.create("Invitation", null, creator.getEmail());
        var invitation = service.invite(group.id(), " " + recipient.toUpperCase(Locale.ROOT) + " ",
                creator.getEmail());
        assertThat(invitation.email()).isEqualTo(recipient);
        assertError("INVITATION_ALREADY_PENDING", () -> service.invite(group.id(), recipient, creator.getEmail()));
        assertError("INVITATION_NOT_FOUND", () -> service.invitationByLink(invitation.token(), outsider.getEmail()));
        assertError("INVITATION_NOT_FOUND", () -> service.accept(invitation.id(), outsider.getEmail()));
        assertThat(jdbc.queryForObject("select token_hash from group_invitations where id = ?", String.class,
                invitation.id())).hasSize(64).isNotEqualTo(invitation.token());

        users.save(new User("Late Sign-up", recipient, "unused"));
        assertThat(service.inbox(recipient)).extracting(GroupInvitationResponse.Received::id).containsExactly(invitation.id());
        assertThat(service.invitationByLink(invitation.token(), recipient).groupName()).isEqualTo("Invitation");
        service.archive(group.id(), creator.getEmail());
        assertError("GROUP_NOT_ACTIVE", () -> service.invitationByLink(invitation.token(), recipient));
        assertError("GROUP_NOT_ACTIVE", () -> service.accept(invitation.id(), recipient));
        service.restore(group.id(), creator.getEmail());

        service.decline(invitation.id(), recipient);
        assertThat(service.invitationByLink(invitation.token(), recipient).status()).isEqualTo(GroupInvitation.Status.DECLINED);
        var replacement = service.invite(group.id(), recipient, creator.getEmail());
        service.revoke(group.id(), replacement.id(), creator.getEmail());
        assertError("INVITATION_NOT_PENDING", () -> service.accept(replacement.id(), recipient));

        var expired = invitations.save(new GroupInvitation(groups.findById(group.id()).orElseThrow(), recipient,
                "0".repeat(64), Instant.now().minusSeconds(60)));
        assertError("INVITATION_EXPIRED", () -> service.accept(expired.getId(), recipient));
        assertThat(service.inbox(recipient)).isEmpty();
        var fresh = service.invite(group.id(), recipient, creator.getEmail());
        assertThat(service.accept(fresh.id(), recipient).memberCount()).isEqualTo(2);
        service.leave(group.id(), false, recipient);
        var oldLink = service.invitationByLink(fresh.token(), recipient);
        assertThat(oldLink.status()).isEqualTo(GroupInvitation.Status.ACCEPTED);
        assertThat(oldLink.groupId()).isNull();
        assertThat(oldLink.groupName()).isNull();
    }

    @Test
    void concurrentAcceptanceCreatesOnlyOneMembership() throws Exception {
        var creator = account("Creator");
        var guest = account("Guest");
        var group = service.create("Race", null, creator.getEmail());
        var invite = service.invite(group.id(), guest.getEmail(), creator.getEmail());
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> acceptanceResult(start, invite.id(), guest.getEmail()));
            var second = executor.submit(() -> acceptanceResult(start, invite.id(), guest.getEmail()));
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("ACCEPTED", "INVITATION_NOT_PENDING");
        } finally {
            executor.shutdownNow();
        }
        assertThat(memberships.countByGroupId(group.id())).isEqualTo(2);
        assertThat(service.activity(group.id(), creator.getEmail(), 0, 10).items())
                .extracting(GroupActivityResponse.Item::type).containsExactly(GroupActivity.Type.JOINED,
                        GroupActivity.Type.CREATED);
    }

    @Test
    void closingAnArchivedGroupKeepsDataAndRevokesRemainingInvitations() {
        var creator = account("Creator");
        var guest = account("Guest");
        var group = service.create("Closing group", null, creator.getEmail());
        var invitation = service.invite(group.id(), guest.getEmail(), creator.getEmail());
        service.archive(group.id(), creator.getEmail());
        assertError("LAST_MEMBER_CONFIRMATION_REQUIRED", () -> service.leave(group.id(), false, creator.getEmail()));
        service.leave(group.id(), true, creator.getEmail());
        assertThat(service.list(creator.getEmail())).isEmpty();
        assertThat(groups.findById(group.id())).get().extracting(ExpenseGroup::getStatus).isEqualTo(GroupStatus.CLOSED);
        assertThat(service.invitationByLink(invitation.token(), guest.getEmail()).status())
                .isEqualTo(GroupInvitation.Status.REVOKED);
        assertError("INVITATION_NOT_PENDING", () -> service.accept(invitation.id(), guest.getEmail()));
        assertError("GROUP_FORBIDDEN", () -> service.details(group.id(), creator.getEmail()));
    }

    private String acceptanceResult(CountDownLatch start, Long invitationId, String email) throws InterruptedException {
        start.await();
        try {
            service.accept(invitationId, email);
            return "ACCEPTED";
        } catch (GroupOperationException exception) {
            return exception.getCode();
        }
    }

    private User account(String name) {
        return users.save(new User(name, uniqueEmail(), "unused"));
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    private static void assertError(String code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(GroupOperationException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
