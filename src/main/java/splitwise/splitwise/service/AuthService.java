package splitwise.splitwise.service;

import splitwise.splitwise.dto.JwtResponse;
import splitwise.splitwise.dto.LoginRequest;
import splitwise.splitwise.dto.SignupRequest;


public interface AuthService {

    public JwtResponse signup(SignupRequest request);

    public JwtResponse login(LoginRequest request);


}

