package splitwise.splitwise.service.impl;

import java.util.List;
import java.util.Optional;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import splitwise.splitwise.dto.UserUpdateRequest;
import splitwise.splitwise.exception.EmailNotFoundException;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.exception.UserWithPendingDues;
import splitwise.splitwise.model.GroupMember;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.ExpenseRepository;
import splitwise.splitwise.repository.ExpenseSplitRepository;
import splitwise.splitwise.repository.GroupMemberRepository;
import splitwise.splitwise.repository.SettlementRepository;
import splitwise.splitwise.repository.UserRepository;
import splitwise.splitwise.service.ExpenseService;
import splitwise.splitwise.service.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseService expenseService;

    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final PasswordEncoder passwordEncoder;

    //create user
   /* public User createUser(User user){
        boolean emailMissing = user.getEmail() == null || user.getEmail().isEmpty();
        boolean phoneMissing = user.getPhone() == null || user.getPhone().isEmpty();
        if (emailMissing && phoneMissing){
            throw new RuntimeException(("Both email and phone are required"));
        } else if (emailMissing) {
            throw new RuntimeException(("Email is required"));
        }else if (phoneMissing) {
            throw new RuntimeException(("Phone is required"));
        }
        return userRepository.save(user);
    }*/

    //update user
    @Override
    public User updateUser(Long userId, UserUpdateRequest updateUser) {
        log.info("Updating user: userId={}", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFound("User not found"));
        user.setUsername(updateUser.getUsername());
        user.setEmail(updateUser.getEmail());
        user.setPhone(updateUser.getPhone());
        user.setPassword(passwordEncoder.encode(updateUser.getPassword()));

        User saved = userRepository.save(user);
        log.info("Updated user saved: userId={}", saved.getUserid());
        return saved;
    }

    @Override
    @Transactional
    public void deleteUser(Long userId){
        log.info("Deleting user: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UserNotFound("User not found"));


        List<GroupMember> userGroups = groupMemberRepository.findById_Userid(userId);

        for(GroupMember member: userGroups){
            Long groupId = member.getId().getGroupid();
            List<String> userBalances = expenseService.getUserBalance(groupId,userId);

            if (!userBalances.isEmpty()){
                throw new UserWithPendingDues("Cannot delete user with pending dues");
            }
        }

        expenseSplitRepository.deleteByUserid(userId);

        expenseRepository.deleteByUserid_Userid(userId);

        settlementRepository.deleteByPaidby_UseridOrPaidto_Userid(userId,userId);

        groupMemberRepository.deleteById_Userid(userId);

        userRepository.deleteById(userId);
        log.info("Deleted user: userId={}", userId);

    }

    // Get user by ID
    @Override
    public Optional<User> getUserById(Long userId){
        log.debug("Fetching user by id: userId={}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFound("user not found"));
        return userRepository.findById(userId);
    }

    @Override
    public List<Long> resolveUserIds(List<String> emails) {
        log.debug("Resolving user ids for emails: count={}", emails.size());
        List<User> users = userRepository.findByEmailIn(emails);

        List<String> foundEmails = users.stream()
                .map(User::getEmail)
                .toList();

        List<String> missingEmails = emails.stream()
                .filter(email -> !foundEmails.contains(email))
                .toList();

        if (!missingEmails.isEmpty()) {
            log.warn("Email(s) not found: {}", String.join(", ", missingEmails));
            throw new EmailNotFoundException("Users not found for emails: " + String.join(", ", missingEmails));
        }

        return users.stream()
                .map(User::getUserid)
                .toList();
    }
}
