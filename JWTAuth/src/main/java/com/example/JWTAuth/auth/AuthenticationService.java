package com.example.JWTAuth.auth;

import com.example.JWTAuth.config.JwtService;
import com.example.JWTAuth.user.Role;
import com.example.JWTAuth.user.User;
import com.example.JWTAuth.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // check if email already exist
        Optional<User> userOptional = repository
                .findByEmail(request.getEmail());
        if (userOptional.isPresent()) {
            return AuthenticationResponse.builder()
                    .message("Email already exist!")
                    .build();
        }

        // create the user based on info from request body
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        repository.save(user);

        // create claim and generate token
        // return token to frontend
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", request.getRole());
        extraClaims.put("firstname", user.getFirstname());
        extraClaims.put("lastname", user.getLastname());

        var jwtToken = jwtService.generateToken(extraClaims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // check if user exist
        Optional<User> userOptional = repository
                .findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            return AuthenticationResponse.builder()
                    .message("Email does not exist!")
                    .build();
        }

        // check if password is matches
        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e){
            return AuthenticationResponse.builder()
                    .message("Invalid password!")
                    .build();
        }

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();

        // create claim and generate token
        // return token to frontend
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("firstname", user.getFirstname());
        extraClaims.put("lastname", user.getLastname());

        var jwtToken = jwtService.generateToken(extraClaims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}
