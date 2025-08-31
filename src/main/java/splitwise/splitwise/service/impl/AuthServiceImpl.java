package splitwise.splitwise.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import splitwise.splitwise.dto.JwtResponse;
import splitwise.splitwise.dto.LoginRequest;
import splitwise.splitwise.dto.SignupRequest;
import splitwise.splitwise.exception.InvalidCredentials;
import splitwise.splitwise.exception.UserAlreadyRegistered;
import splitwise.splitwise.exception.UserNotFound;
import splitwise.splitwise.model.User;
import splitwise.splitwise.repository.UserRepository;
import splitwise.splitwise.service.AuthService;
import splitwise.splitwise.utility.JwtUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public JwtResponse signup(SignupRequest request) {
        log.info("Signup attempt for username={}, email={}", request.getUsername(), request.getEmail());
        if(userRepository.existsByEmail(request.getEmail())){
            log.warn("Signup failed: email already registered: {}", request.getEmail());
            throw new UserAlreadyRegistered("User is already registered with this email");
        }

        if(userRepository.existsByPhone(request.getPhone())){
            log.warn("Signup failed: phone already registered: {}", request.getPhone());
            throw new UserAlreadyRegistered("User is already registered with this number");
        }

        if(userRepository.existsByUsername(request.getUsername())){
            log.warn("Signup failed: username already registered: {}", request.getUsername());
            throw new UserAlreadyRegistered("User is already registered with this username");
        }


        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getUserid());
        log.info("Signup success for userId={}, email={}", savedUser.getUserid(), savedUser.getEmail());
        return new JwtResponse(token);
    }

    @Override
    public JwtResponse login(LoginRequest request){
        log.info("Login attempt for identifier={}", request.getIdentifier());
        User user = userRepository.findByEmailOrPhone(request.getIdentifier(), request.getIdentifier())
                .orElseThrow(()->new UserNotFound("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            log.warn("Login failed: invalid credentials for userId={}", user.getUserid());
            throw new InvalidCredentials("Invaild credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getUserid());
        log.info("Login success for userId={}", user.getUserid());
        return new JwtResponse(token);
    }

}


