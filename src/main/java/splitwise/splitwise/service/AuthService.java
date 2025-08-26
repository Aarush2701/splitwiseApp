package splitwise.splitwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import splitwise.splitwise.dto.JwtResponse;
import splitwise.splitwise.dto.LoginRequest;
import splitwise.splitwise.dto.SignupRequest;
import splitwise.splitwise.exception.InvalidCredentials;
import splitwise.splitwise.exception.UserAlreadyRegistered;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.UserRepository;
import splitwise.splitwise.utility.JwtUtil;


public interface AuthService {

    public JwtResponse signup(SignupRequest request);

    public JwtResponse login(LoginRequest request);


}

