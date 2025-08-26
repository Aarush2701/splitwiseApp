package splitwise.splitwise.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import splitwise.splitwise.dto.UserUpdateRequest;
import splitwise.splitwise.exception.EmailNotFoundException;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.exception.UserWithPendingDues;
import splitwise.splitwise.model.GroupMember;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


public interface UserService {

    public User updateUser(Long userId, UserUpdateRequest updateUser);

    public void deleteUser(Long userId);

    public Optional<User> getUserById(Long userId);

    public List<Long> resolveUserIds(List<String> emails);

}
