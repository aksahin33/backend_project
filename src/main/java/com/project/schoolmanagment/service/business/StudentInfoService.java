package com.project.schoolmanagment.service.business;

import com.project.schoolmanagment.entity.concretes.business.EducationTerm;
import com.project.schoolmanagment.entity.concretes.business.Lesson;
import com.project.schoolmanagment.entity.concretes.business.StudentInfo;
import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.entity.enums.Note;
import com.project.schoolmanagment.entity.enums.RoleType;
import com.project.schoolmanagment.exception.ConflictException;
import com.project.schoolmanagment.exception.ResourceNotFoundException;
import com.project.schoolmanagment.payload.mappers.StudentInfoMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.messages.SuccessMessages;
import com.project.schoolmanagment.payload.request.business.StudentInfoRequest;
import com.project.schoolmanagment.payload.request.business.UpdateStudentInfoRequest;
import com.project.schoolmanagment.payload.response.abstracts.ResponseMessage;
import com.project.schoolmanagment.payload.response.business.StudentInfoResponse;
import com.project.schoolmanagment.repository.business.StudentInfoRepository;
import com.project.schoolmanagment.service.helper.MethodHelper;
import com.project.schoolmanagment.service.helper.PageableHelper;
import com.project.schoolmanagment.service.user.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class StudentInfoService {

    private final StudentInfoRepository studentInfoRepository;
    private final StudentService studentService;
    private final LessonService lessonService;
    private final MethodHelper methodHelper;
    private final EducationTermService educationTermService;
    private final StudentInfoMapper studentInfoMapper;
    private final PageableHelper pageableHelper;

    @Value("${midterm.exam.impact.percentage}")
    private Double midtermExamPercentage;
    @Value("${final.exam.impact.percentage}")
    private Double finalExamPercentage;



    public ResponseMessage<StudentInfoResponse> saveStudentInfo(
            HttpServletRequest request,
            StudentInfoRequest studentInfoRequest) {

        String teacherUsername = (String) request.getAttribute("username");
        //get student
        User student = methodHelper.isUserExist(studentInfoRequest.getStudentId());
        //validate user is a student
        methodHelper.checkRole(student, RoleType.STUDENT);
        //get teacher
        User teacher = methodHelper.isUserExistByUsername(teacherUsername);
        //get lesson
        Lesson lesson = lessonService.isLessonExistById(studentInfoRequest.getLessonId());
        //get education term
        EducationTerm educationTerm1 = educationTermService.isEducationTermExist(studentInfoRequest.getEducationTermId());

        //A student may have only one Student INFO related to one lesson
        isDuplicatedLessonAndInfo(student.getId(), lesson.getLessonName());

        //calculate the average note and get the suitable note
        Double average = calculateAverageNote(  studentInfoRequest.getMidtermExam(),
                                                studentInfoRequest.getFinalExam());
        Note note = checkLetterGrade(average);

        //map DTO to domain object
        StudentInfo studentInfo = studentInfoMapper.mapStudentInfoRequestToStudentInfo(studentInfoRequest, note, average);

        //set missing props
        studentInfo.setStudent(student);
        studentInfo.setEducationTerm(educationTerm1);
        studentInfo.setTeacher(teacher);
        studentInfo.setLesson(lesson);
        StudentInfo savedStudentInfo = studentInfoRepository.save(studentInfo);

        return ResponseMessage.<StudentInfoResponse>builder()
                .message(SuccessMessages.STUDENT_INFO_SAVE)
                .object(studentInfoMapper.mapStudentInfoToStudentInfoResponse(savedStudentInfo))
                .httpStatus(HttpStatus.CREATED)
                .build();
    }



    private void isDuplicatedLessonAndInfo(Long studentId, String lessonName) {

        boolean isLessonDuplicationExist = studentInfoRepository.getAllByStudentId_Id(studentId)
                .stream()
                .anyMatch(info -> info.getLesson().getLessonName().equalsIgnoreCase(lessonName));

        if (isLessonDuplicationExist) {
            throw new ConflictException(String.format(ErrorMessages.ALREADY_REGISTER_LESSON_MESSAGE, lessonName));
        }
    }

    //%40-%60
    private Double calculateAverageNote(Double midtermExam, Double finalExam){
        return (midtermExam*midtermExamPercentage)+(finalExam*finalExamPercentage);
    }


    private Note checkLetterGrade(Double average){

        if(average<50.0) {
            return Note.FF;
        } else if (average<60) {
            return Note.DD;
        } else if (average<65) {
            return Note.CC;
        } else if (average<70) {
            return  Note.CB;
        } else if (average<75) {
            return  Note.BB;
        } else if (average<80) {
            return Note.BA;
        } else {
            return Note.AA;
        }
    }


    public ResponseMessage deleteById(Long studentInfoId) {

        StudentInfo studentInfo = isStudentInfoExist(studentInfoId);
        studentInfoRepository.delete(studentInfo);
        return ResponseMessage.builder()
                .message(SuccessMessages.STUDENT_INFO_DELETE)
                .httpStatus(HttpStatus.OK)
                .build();
    }


    public StudentInfo isStudentInfoExist(Long id){
        boolean isExist = studentInfoRepository.existsByIdEquals(id);

        if(!isExist){
            throw new ResourceNotFoundException(String.format(ErrorMessages.STUDENT_INFO_NOT_FOUND, id));
        }
        else {
            return studentInfoRepository.findById(id).get();
        }
    }

    public Page<StudentInfoResponse> getStudentInfoByPage(int page, int size, String sort, String type) {
        Pageable pageable = pageableHelper.getPageableWithProperties(page,size,sort,type);
        return studentInfoRepository.findAll(pageable)
                .map(studentInfoMapper::mapStudentInfoToStudentInfoResponse);
    }

    public ResponseMessage<StudentInfoResponse> update(UpdateStudentInfoRequest studentInfoRequest, Long studentInfoId) {

        StudentInfo studentInfo = isStudentInfoExist(studentInfoId);

        Lesson lesson = lessonService.isLessonExistById(studentInfoRequest.getLessonId());

        EducationTerm educationTerm = educationTermService.isEducationTermExist(studentInfoRequest.getEducationTermId());

        Double averageNote = calculateAverageNote(studentInfoRequest.getMidtermExam(), studentInfoRequest.getFinalExam());

        Note note = checkLetterGrade(averageNote);

        StudentInfo mappedStudentInfo = studentInfoMapper
                .mapUpdateStudentInfoRequestToStudentInfo( studentInfoRequest,
                                                            studentInfoId,
                                                            lesson, educationTerm, note, averageNote);

        mappedStudentInfo.setStudent(studentInfo.getStudent());
        mappedStudentInfo.setTeacher(studentInfo.getTeacher());
        StudentInfo savedStudentInfo = studentInfoRepository.save(mappedStudentInfo);

        return ResponseMessage.<StudentInfoResponse>builder()
                .message(SuccessMessages.STUDENT_INFO_UPDATE)
                .object(studentInfoMapper.mapStudentInfoToStudentInfoResponse(savedStudentInfo))
                .httpStatus(HttpStatus.OK)
                .build();

    }

    public Page<StudentInfoResponse> getAllForTeacher(HttpServletRequest request, int page, int size) {

        Pageable pageable = pageableHelper.getPageableWithProperties(page, size);

        String username = (String) request.getAttribute("username");

        //     studentInfoRepository.findAll()
//           .stream()
//           .filter(x->x.getStudent().getUsername().equals(username))
//           .map(studentInfoMapper::mapStudentInfoToStudentInfoResponse);


        return studentInfoRepository
                .findByTeacherUsername(username,pageable)
                .map(studentInfoMapper::mapStudentInfoToStudentInfoResponse);
    }


    public Page<StudentInfoResponse> getAllForStudent(HttpServletRequest request, int page, int size) {

        Pageable pageable = pageableHelper.getPageableWithProperties(page, size);

        String username = (String) request.getAttribute("username");

        return studentInfoRepository
                .findByStudentUsername(username,pageable)
                .map(studentInfoMapper::mapStudentInfoToStudentInfoResponse);
    }

    public List<StudentInfoResponse> getByStudentInfoByStudentId(Long studentId) {

        User student = methodHelper.isUserExist(studentId);

        methodHelper.checkRole(student, RoleType.STUDENT);

        if(!studentInfoRepository.existByStudentId(studentId)){
            throw new ResourceNotFoundException(
                    String.format(ErrorMessages.STUDENT_INFO_NOT_FOUND_BY_STUDENT_ID, studentId));
        }

        return studentInfoRepository.getAllByStudentId_Id(studentId)
                .stream()
                .map(studentInfoMapper::mapStudentInfoToStudentInfoResponse)
                .collect(Collectors.toList());
    }

    public StudentInfoResponse findById(Long id) {

        return studentInfoMapper.mapStudentInfoToStudentInfoResponse(isStudentInfoExist(id));
    }
}
