package com.project.schoolmanagment.service.user;

import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.exception.BadRequestException;
import com.project.schoolmanagment.payload.mappers.UserMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.request.user.LoginRequest;
import com.project.schoolmanagment.payload.request.user.PasswordUpdateRequest;
import com.project.schoolmanagment.payload.response.user.LoginResponse;
import com.project.schoolmanagment.repository.user.UserRepository;
import com.project.schoolmanagment.security.jwt.JwtUtils;
import com.project.schoolmanagment.security.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtUtils jwtUtils;

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final UserMapper userMapper;



    public ResponseEntity<LoginResponse> authenticateUser(LoginRequest request) {

        String username = request.getUsername();
        String password = request.getPassword();

        //we will validate the username and password
        Authentication authentication =
                authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(username,password));

        //we are uploading the user information into security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = "Bearer "+jwtUtils.generateJwtToken(authentication);

        //we are getting logged in user information
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        //we are getting the roles of this user
        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        //since we have only one role, we are getting the first value
        Optional<String> role = roles.stream().findFirst();

        LoginResponse.LoginResponseBuilder loginResponseBuilder = LoginResponse.builder();

        loginResponseBuilder.username(userDetails.getUsername());
        loginResponseBuilder.token(token.substring(7));
        loginResponseBuilder.name(userDetails.getName());
        loginResponseBuilder.ssn(userDetails.getSsn());
        role.ifPresent(loginResponseBuilder::role);

        return ResponseEntity.ok(loginResponseBuilder.build());

    }

    public void updatePassword(PasswordUpdateRequest passwordUpdateRequest, HttpServletRequest request) {

        String userName = (String) request.getAttribute("username");
        User user = userRepository.findByUsername(userName);

        if(user.getBuiltIn()){
            throw new BadRequestException(ErrorMessages.NOT_PERMITTED_METHOD_MESSAGE);
        }

        if(!passwordEncoder.matches(passwordUpdateRequest.getOldPassword(), user.getPassword())){
            throw new BadRequestException(ErrorMessages.PASSWORD_NOT_MATCHED);
        }

        user.setPassword(passwordEncoder.encode(passwordUpdateRequest.getNewPassword()));

        userRepository.save(user);
    }
}
