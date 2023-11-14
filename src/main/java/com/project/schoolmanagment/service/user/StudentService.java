package com.project.schoolmanagment.service.user;

import com.project.schoolmanagment.entity.concretes.business.LessonProgram;
import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.entity.enums.RoleType;
import com.project.schoolmanagment.payload.mappers.UserMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.messages.SuccessMessages;
import com.project.schoolmanagment.payload.request.user.ChooseLessonProgramWithId;
import com.project.schoolmanagment.payload.request.user.StudentRequest;
import com.project.schoolmanagment.payload.request.user.StudentRequestWithoutPassword;
import com.project.schoolmanagment.payload.response.abstracts.ResponseMessage;
import com.project.schoolmanagment.payload.response.user.StudentResponse;
import com.project.schoolmanagment.repository.user.UserRepository;
import com.project.schoolmanagment.service.business.LessonProgramService;
import com.project.schoolmanagment.service.helper.MethodHelper;
import com.project.schoolmanagment.service.validator.DateTimeValidator;
import com.project.schoolmanagment.service.validator.UniquePropertyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final UserRepository userRepository;
    private final MethodHelper methodHelper;
    private final UniquePropertyValidator uniquePropertyValidator;
    private final UserMapper userMapper;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final LessonProgramService lessonProgramService;
    private final DateTimeValidator dateTimeValidator;

    public ResponseMessage<StudentResponse> saveStudent(StudentRequest studentRequest) {
        //check DB if advisor teacher exist
        User advisorTeacher = methodHelper.isUserExist(studentRequest.getAdvisorTeacherId());
        //check if it is an advisor teacher
        methodHelper.checkAdvisor(advisorTeacher);
        //validate unique props
        uniquePropertyValidator.checkDuplicate( studentRequest.getUsername(),
                                                studentRequest.getSsn(),
                                                studentRequest.getPhoneNumber(),
                                                studentRequest.getEmail());

        User student = userMapper.mapStudentRequestToUser(studentRequest);

        student.setAdvisorTeacherId(advisorTeacher.getAdvisorTeacherId());
        student.setUserRole(userRoleService.getUserRole(RoleType.STUDENT));
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        student.setActive(true);
        student.setIsAdvisor(false);
        student.setStudentNumber(getLastNumber());

        User savedStudent = userRepository.save(student);

        return ResponseMessage.<StudentResponse>builder()
                .object(userMapper.mapUserToStudentResponse(savedStudent))
                .message(SuccessMessages.STUDENT_SAVE)
                .build();

    }

    private int getLastNumber(){

        if(!userRepository.findUsersByRoleType(RoleType.STUDENT)){
            return 1000;
        }else {
            return userRepository.getMaxStudentNumber()+1;
        }

    }

    public ResponseEntity<String> updateStudent(StudentRequestWithoutPassword studentRequestWithoutPassword,
                                                HttpServletRequest request) {

        String username = (String) request.getAttribute("username");
        //fetch user information from db if we have a user like this
        User student = userRepository.findByUsername(username);
        //validate props for uniqueness
        uniquePropertyValidator.checkUniqueProperties(student, studentRequestWithoutPassword);

        //ordinary way of mapping
        student.setMotherName(studentRequestWithoutPassword.getMotherName());
        student.setFatherName(studentRequestWithoutPassword.getFatherName());
        student.setGender(studentRequestWithoutPassword.getGender());
        student.setSurname(studentRequestWithoutPassword.getSurname());
        student.setSsn(studentRequestWithoutPassword.getSsn());
        student.setEmail(studentRequestWithoutPassword.getEmail());
        student.setPhoneNumber(studentRequestWithoutPassword.getPhoneNumber());
        student.setBirthDay(studentRequestWithoutPassword.getBirthDay());
        student.setBirthPlace(studentRequestWithoutPassword.getBirthPlace());
        student.setName(studentRequestWithoutPassword.getName());
        student.setUsername(studentRequestWithoutPassword.getUsername());

        userRepository.save(student);

        return ResponseEntity.ok(SuccessMessages.STUDENT_UPDATE);



    }

    public ResponseMessage<StudentResponse> updateStudentForManagers(Long id, StudentRequest studentRequest) {
        //check if we have this student in db
        User student = methodHelper.isUserExist(id);
        //check if it is a student
        methodHelper.checkRole(student, RoleType.STUDENT);
        //validate unique properties
        uniquePropertyValidator.checkUniqueProperties(student,studentRequest);
        //map DTO to user
        User studentFromMapper = userMapper.mapStudentRequestToUpdatedUser(studentRequest, id);
        //mapping other properties
        studentFromMapper.setPassword(student.getPassword());
        studentFromMapper.setAdvisorTeacherId(studentRequest.getAdvisorTeacherId());
        studentFromMapper.setStudentNumber(student.getStudentNumber());
        studentFromMapper.setUserRole(userRoleService.getUserRole(RoleType.STUDENT));
        studentFromMapper.setActive(true);

        return ResponseMessage.<StudentResponse>builder()
                .message(SuccessMessages.STUDENT_UPDATE)
                .object(userMapper.mapUserToStudentResponse(userRepository.save(studentFromMapper)))
                .httpStatus(HttpStatus.OK)
                .build();
    }

    public ResponseMessage<StudentResponse> addLessonProgram(HttpServletRequest request,
                        ChooseLessonProgramWithId chooseLessonProgramWithId) {

        User student = methodHelper.isUserExistByUsername((String) request.getAttribute("username"));

        Set<LessonProgram> lessonProgramSet = lessonProgramService
                .getLessonProgramById(chooseLessonProgramWithId.getLessonProgramId());
        Set<LessonProgram> lessonProgramsFromUserDb = student.getLessonProgramList();
        lessonProgramsFromUserDb.addAll(lessonProgramSet);

        dateTimeValidator.checkDuplicateLessonPrograms(lessonProgramsFromUserDb);

        student.setLessonProgramList(lessonProgramsFromUserDb);

        User updatedStudent = userRepository.save(student);

        return ResponseMessage.<StudentResponse>builder()
                .message(SuccessMessages.LESSON_PROGRAM_ADD_TO_STUDENT)
                .object(userMapper.mapUserToStudentResponse(updatedStudent))
                .httpStatus(HttpStatus.OK)
                .build();
    }

    public ResponseMessage changeStatusOfStudent(Long id, boolean status) {

        User student = methodHelper.isUserExist(id);
        methodHelper.checkRole(student, RoleType.STUDENT);

        student.setActive(status);
        userRepository.save(student);

        return ResponseMessage.builder()
                .message("Student is "+ (status ? "active" : "passive"))
                .httpStatus(HttpStatus.OK)
                .build();

    }
}
