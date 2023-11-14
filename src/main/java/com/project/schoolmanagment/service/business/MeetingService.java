package com.project.schoolmanagment.service.business;

import com.project.schoolmanagment.entity.concretes.business.Meet;
import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.entity.enums.RoleType;
import com.project.schoolmanagment.exception.BadRequestException;
import com.project.schoolmanagment.exception.ConflictException;
import com.project.schoolmanagment.exception.ResourceNotFoundException;
import com.project.schoolmanagment.payload.mappers.MeetingMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.messages.SuccessMessages;
import com.project.schoolmanagment.payload.request.business.MeetingRequest;
import com.project.schoolmanagment.payload.response.abstracts.ResponseMessage;
import com.project.schoolmanagment.payload.response.business.MeetingResponse;
import com.project.schoolmanagment.repository.business.MeetingRepository;
import com.project.schoolmanagment.service.helper.MethodHelper;
import com.project.schoolmanagment.service.helper.PageableHelper;
import com.project.schoolmanagment.service.user.UserService;
import com.project.schoolmanagment.service.validator.DateTimeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserService userService;
    private final MethodHelper methodHelper;
    private final DateTimeValidator dateTimeValidator;
    private final MeetingMapper meetingMapper;
    private final PageableHelper pageableHelper;

    public ResponseMessage<MeetingResponse> saveMeeting(HttpServletRequest request, MeetingRequest meetingRequest) {

        String username =(String) request.getAttribute("username");
        //validate if teacher
        User advisorTeacher = methodHelper.isUserExistByUsername(username);
        //validate if advisor
        methodHelper.checkAdvisor(advisorTeacher);

        dateTimeValidator.checkTimeWithException(meetingRequest.getStartTime(), meetingRequest.getStopTime());

        checkMeetingConflicts(  advisorTeacher.getId(),
                                meetingRequest.getDate(),
                                meetingRequest.getStartTime(),
                                meetingRequest.getStopTime());

        List<User> students = userService.findUsersByIdArray(meetingRequest.getStudentIds());

        for(User student : students){
            methodHelper.checkRole(student,RoleType.STUDENT);
        }

        Meet meet = meetingMapper.mapMeetRequestToMeet(meetingRequest);

        meet.setStudentList(students);
        meet.setAdvisoryTeacher(advisorTeacher);

        Meet savedMeet = meetingRepository.save(meet);

        return ResponseMessage.<MeetingResponse>builder()
                .message(SuccessMessages.MEET_SAVE)
                .object(meetingMapper.mapMeetToMeetResponse(savedMeet))
                .httpStatus(HttpStatus.CREATED)
                .build();





    }


    private void checkMeetingConflicts(Long userId, LocalDate date, LocalTime startTime, LocalTime stopTime){

        List<Meet> meets;

        //if teacher or student
        if(Boolean.TRUE.equals(methodHelper.isUserExist(userId).getIsAdvisor())){
            meets = meetingRepository.findByAdvisoryTeacher_IdEquals(userId);

        }else{
            meets = meetingRepository.findByStudentList_IdEquals(userId);
        }

        //conflict validation
        for(Meet meet:meets){
            LocalTime existingStartTime = meet.getStartTime();
            LocalTime existingStopTime = meet.getStopTime();

            if(meet.getDate().equals(date)
                &&
                    (
                        startTime.isAfter(existingStartTime) && startTime.isBefore(existingStopTime) ||
                        stopTime.isAfter(existingStartTime) && stopTime.isBefore(existingStopTime)  ||
                        startTime.isBefore(existingStartTime) && stopTime.isAfter(existingStopTime) ||
                        startTime.equals(existingStartTime) || stopTime.equals(existingStopTime))){

                throw new ConflictException(ErrorMessages.MEET_HOURS_CONFLICT);
            }

        }

    }

    public List<MeetingResponse> getAll() {

        return meetingRepository.findAll()
                .stream()
                .map(meetingMapper::mapMeetToMeetResponse)
                .collect(Collectors.toList());
    }

    public ResponseMessage<MeetingResponse> getMeetingById(Long id) {
        Meet meet = isMeetExist(id);

        return ResponseMessage.<MeetingResponse>builder()
                .message(SuccessMessages.MEET_FOUND)
                .httpStatus(HttpStatus.OK)
                .object(meetingMapper.mapMeetToMeetResponse(meet))
                .build();
    }

    private Meet isMeetExist(Long id){
        return meetingRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException(
                        String.format(ErrorMessages.MEET_NOT_FOUND_MESSAGE, id)));
    }

    public ResponseMessage deleteById(Long id) {

        Meet meet = isMeetExist(id);
        meetingRepository.delete(meet);
        
        return ResponseMessage.builder()
                .message(SuccessMessages.MEET_DELETE)
                .httpStatus(HttpStatus.OK)
                .build();
    }

    public ResponseMessage<MeetingResponse> updateMeeting(Long meetingId,
                                                          MeetingRequest meetingRequest,
                                                          HttpServletRequest request) {

        Meet meet = isMeetExist(meetingId);

        //validate if teacher is updating his/her own meeting
        isMeetingAssignToThisTeacher(meet, request);

        //validate the time
        dateTimeValidator.checkTimeWithException(meetingRequest.getStartTime(), meetingRequest.getStopTime());

        if(         meet.getDate().equals(meetingRequest.getDate()) &&
                    meet.getStartTime().equals(meetingRequest.getStartTime()) &&
                    meet.getStopTime().equals(meetingRequest.getStopTime())
                    )   {
            //conflicts related to students
            for(Long studentId: meetingRequest.getStudentIds()){
                checkMeetingConflicts(studentId,
                        meetingRequest.getDate(),
                        meetingRequest.getStartTime(),
                        meetingRequest.getStopTime());
            }

            //conflicts related to the teacher
            checkMeetingConflicts(meet.getAdvisoryTeacher().getId(),
                    meetingRequest.getDate(),
                    meetingRequest.getStartTime(),
                    meetingRequest.getStopTime());

        }

        List<User> students = userService.findUsersByIdArray(meetingRequest.getStudentIds());
        Meet updateMeet = meetingMapper.mapUpdateRequestToMeet(meetingRequest, meetingId);

        updateMeet.setStudentList(students);
        updateMeet.setAdvisoryTeacher(meet.getAdvisoryTeacher());
        Meet savedMeeting = meetingRepository.save(updateMeet);
        return ResponseMessage.<MeetingResponse>builder()
                .message(SuccessMessages.MEET_UPDATE)
                .httpStatus(HttpStatus.OK)
                .object(meetingMapper.mapMeetToMeetResponse(savedMeeting))
                .build();
    }

    private void isMeetingAssignToThisTeacher(Meet meet, HttpServletRequest request){

        String username = (String) request.getAttribute("username");
        User user = methodHelper.isUserExistByUsername(username);
        if(user.getUserRole().getRoleType().getName().equals("Teacher") &&
                (meet.getAdvisoryTeacher().getAdvisorTeacherId()!=(user.getId()))){
                throw new BadRequestException(ErrorMessages.NOT_PERMITTED_METHOD_MESSAGE);

        }
    }

    public ResponseEntity<List<MeetingResponse>> getAllMeetByTeacher(HttpServletRequest request) {

        String username = (String) request.getAttribute("username");

        User advisorTeacher = methodHelper.isUserExistByUsername(username);

        methodHelper.checkAdvisor(advisorTeacher);

        List<MeetingResponse> meetingResponseList =
                meetingRepository.findByAdvisoryTeacher_IdEquals(advisorTeacher.getId())
                        .stream()
                        .map(meetingMapper::mapMeetToMeetResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(meetingResponseList);
    }



    public ResponseEntity<List<MeetingResponse>> getAllMeetByStudent(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");

        User student = methodHelper.isUserExistByUsername(username);

        List<MeetingResponse> meetingResponseList =
                meetingRepository.findByStudentList_IdEquals(student.getId())
                        .stream()
                        .map(meetingMapper::mapMeetToMeetResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(meetingResponseList);
    }

    public Page<MeetingResponse> getAllMeetByPage(int page, int size) {
        Pageable pageable = pageableHelper.getPageableWithProperties(page,size);

        return meetingRepository.findAll(pageable)
                .map(meetingMapper::mapMeetToMeetResponse);

    }

    public Page<MeetingResponse> getAllMeetByAdvisorAsPage(HttpServletRequest request, int page, int size) {
        String username = (String) request.getAttribute("username");

        User teacher = methodHelper.isUserExistByUsername(username);

        methodHelper.checkAdvisor(teacher);

        Pageable pageable = pageableHelper.getPageableWithProperties(page,size);

        return meetingRepository.findByAdvisoryTeacher_IdEquals(teacher.getId(),pageable)
                .map(meetingMapper::mapMeetToMeetResponse);
    }
}
