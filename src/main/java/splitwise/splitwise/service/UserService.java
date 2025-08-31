package splitwise.splitwise.service;

import java.util.List;
import java.util.Optional;

import splitwise.splitwise.dto.UserUpdateRequest;
import splitwise.splitwise.model.User;


public interface UserService {

    public User updateUser(Long userId, UserUpdateRequest updateUser);

    public void deleteUser(Long userId);

    public Optional<User> getUserById(Long userId);

    public List<Long> resolveUserIds(List<String> emails);

}
