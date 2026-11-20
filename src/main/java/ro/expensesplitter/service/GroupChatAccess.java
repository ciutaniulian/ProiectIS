package ro.expensesplitter.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ro.expensesplitter.repository.GroupMembershipRepository;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
public class GroupChatAccess {
    private final GroupMembershipRepository memberships;

    public GroupChatAccess(GroupMembershipRepository memberships) {
        this.memberships = memberships;
    }

    public void requireMember(Long groupId, Principal principal) {
        if (!isMember(groupId, principal)) {
            throw new AccessDeniedException("Group membership required");
        }
    }

    public boolean isMember(Long groupId, Principal principal) {
        return principal != null && memberships.existsByGroupIdAndUserEmail(groupId, principal.getName());
    }

    public Optional<Long> membershipId(Long groupId, Principal principal) {
        return principal == null ? Optional.empty() : memberships.findMembershipId(groupId, principal.getName());
    }

    public List<Long> groupIds(Principal principal) {
        return memberships.findGroupIdsByEmail(principal.getName());
    }
}
