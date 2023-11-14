package com.project.schoolmanagment.service.user;

import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.entity.enums.RoleType;
import com.project.schoolmanagment.exception.ResourceNotFoundException;
import com.project.schoolmanagment.payload.mappers.UserMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.messages.SuccessMessages;
import com.project.schoolmanagment.payload.request.user.UserRequest;
import com.project.schoolmanagment.payload.request.user.UserRequestWithoutPassword;
import com.project.schoolmanagment.payload.response.abstracts.BaseUserResponse;
import com.project.schoolmanagment.payload.response.abstracts.ResponseMessage;
import com.project.schoolmanagment.payload.response.user.UserResponse;
import com.project.schoolmanagment.repository.user.UserRepository;
import com.project.schoolmanagment.service.helper.MethodHelper;
import com.project.schoolmanagment.service.helper.PageableHelper;
import com.project.schoolmanagment.service.validator.UniquePropertyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UniquePropertyValidator uniquePropertyValidator;

    private final UserMapper userMapper;

    private final UserRoleService userRoleService;

    private final PageableHelper pageableHelper;

    private final MethodHelper methodHelper;

    //after security dependency usage
    private final PasswordEncoder passwordEncoder;


    public ResponseMessage<UserResponse> saveUser(UserRequest userRequest, String userRole) {

        //handling uniqueness exceptions
        uniquePropertyValidator.checkDuplicate(
                userRequest.getUsername(),
                userRequest.getSsn(),
                userRequest.getPhoneNumber(),
                userRequest.getEmail());

        //map dto to entity (domain object)
        User user = userMapper.mapUserRequestToUser(userRequest);

        //get correct role from db and set it to the user
        if (userRole.equalsIgnoreCase(RoleType.ADMIN.name)){
            //we are setting a superUser that can not be deleted
            if(Objects.equals(userRequest.getUsername(), "Admin")){
                user.setBuiltIn(true);
            }
            user.setUserRole(userRoleService.getUserRole(RoleType.ADMIN));
        }else if(userRole.equalsIgnoreCase("Dean")){
            user.setUserRole(userRoleService.getUserRole(RoleType.MANAGER));
        } else if (userRole.equalsIgnoreCase("ViceDean")) {
            user.setUserRole(userRoleService.getUserRole(RoleType.ASSISTANT_MANAGER));
        } else {
            throw new ResourceNotFoundException(String.format(ErrorMessages.NOT_FOUND_USER_USER_ROLE_MESSAGE, userRole));

        }

        //this line should be written after security
        user.setPassword(passwordEncoder.encode(user.getPassword()));


        user.setIsAdvisor(false);
        User savedUser = userRepository.save(user);

        return ResponseMessage.<UserResponse>builder()
                .message(SuccessMessages.USER_CREATE)
                .object(userMapper.mapUserToUserResponse(savedUser))
                .build();

    }


    /**
     *
     * @param userId for query
     * @return mapped DTO BaseUserResponse
     */
    public ResponseMessage<BaseUserResponse> getUserById(Long userId) {

        //exception handling if no user exists in DB
        User user = userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException(String.format(ErrorMessages.NOT_FOUND_USER_MESSAGE, userId)));

        //we  neeed to call correct mappers
        BaseUserResponse baseUserResponse;
        if(user.getUserRole().getRoleType()==RoleType.STUDENT){
            baseUserResponse = userMapper.mapUserToStudentResponse(user);
        } else if (user.getUserRole().getRoleType()==RoleType.TEACHER) {
            baseUserResponse = userMapper.mapUserToTeacherResponse(user);
        } else  {
            baseUserResponse = userMapper.mapUserToUserResponse(user);
        }

        return ResponseMessage.<BaseUserResponse>builder()
                .message(SuccessMessages.USER_FOUND)
                .httpStatus(HttpStatus.OK)
                .object(baseUserResponse)
                .build();

    }

    public Page<UserResponse> getUSersByPage(int page, int size, String sort, String type, String userRole) {

        Pageable pageable = pageableHelper.getPageableWithProperties(page,size,sort,type);

        return userRepository.findUserByRole(userRole,pageable)
                .map(userMapper::mapUserToUserResponse);

    }

    public List<UserResponse> getUserByName(String userName) {

        return userRepository.getUserByNameContaining(userName)
                .stream()
                .map(userMapper::mapUserToUserResponse)
                .collect(Collectors.toList());



    }


    //Normally ResponseEntity should be in controller layer.
    public ResponseEntity<String> updateUserForUsers(UserRequestWithoutPassword userRequestWithoutPassword, HttpServletRequest request) {

        String userName = (String) request.getAttribute("username");

        User user= userRepository.findByUsername(userName);

        //we need to check if this user can be changed
        methodHelper.isUserBuiltIn(user);

        //we need to check if we are changing the unique properties
        uniquePropertyValidator.checkUniqueProperties(user, userRequestWithoutPassword);

        user.setUsername(userRequestWithoutPassword.getUsername());
        user.setBirthDay(userRequestWithoutPassword.getBirthDay());
        user.setEmail(userRequestWithoutPassword.getEmail());
        user.setPhoneNumber(userRequestWithoutPassword.getPhoneNumber());
        user.setBirthPlace(userRequestWithoutPassword.getBirthPlace());
        user.setGender(userRequestWithoutPassword.getGender());
        user.setName(userRequestWithoutPassword.getName());
        user.setSurname(userRequestWithoutPassword.getSurname());
        user.setSsn(userRequestWithoutPassword.getSsn());
        userRepository.save(user);

        String message = SuccessMessages.USER_UPDATE;

        return new ResponseEntity<>(message, HttpStatus.OK);

    }


    public List<User> findUsersByIdArray(Long[] userId){
        return userRepository.findUsersByIdArray(userId);

    }
}
