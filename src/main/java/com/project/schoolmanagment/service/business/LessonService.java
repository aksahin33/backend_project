package com.project.schoolmanagment.service.business;

import com.project.schoolmanagment.entity.concretes.business.Lesson;
import com.project.schoolmanagment.exception.ConflictException;
import com.project.schoolmanagment.exception.ResourceNotFoundException;
import com.project.schoolmanagment.payload.mappers.LessonMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.messages.SuccessMessages;
import com.project.schoolmanagment.payload.request.business.LessonRequest;
import com.project.schoolmanagment.payload.response.abstracts.ResponseMessage;
import com.project.schoolmanagment.payload.response.business.LessonResponse;
import com.project.schoolmanagment.repository.business.LessonRepository;
import com.project.schoolmanagment.service.helper.PageableHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final PageableHelper pageableHelper;

    public ResponseMessage<LessonResponse> saveLesson(LessonRequest lessonRequest) {

        //only one lesson should exist according to name of the lesson
        isLessonExistByLessonName(lessonRequest.getLessonName());

        Lesson lesson = lessonMapper.mapLessonRequestToLesson(lessonRequest);

        Lesson savedLesson = lessonRepository.save(lesson);

        return ResponseMessage.<LessonResponse>builder()
                .object(lessonMapper.mapLessonToLessonResponse(savedLesson))
                .message(SuccessMessages.LESSON_SAVE)
                .httpStatus(HttpStatus.CREATED)
                .build();
    }

    /**
     * exceptionhandler method for lessonName
     * @param lessonName to search
     * @return true if does not exist
     */
    private boolean isLessonExistByLessonName(String lessonName){
        boolean lessonExist = lessonRepository.existsByLessonNameEqualsIgnoreCase(lessonName);

        if(lessonExist){
            throw new ConflictException(String.format(ErrorMessages.ALREADY_REGISTER_LESSON_MESSAGE,lessonName));
        }else{
            return true;
        }
    }



    public ResponseMessage deleteLessonById(Long id) {

        isLessonExistById(id);

        lessonRepository.deleteById(id);

        return ResponseMessage.builder()
                .message(SuccessMessages.LESSON_DELETE)
                .httpStatus(HttpStatus.OK)
                .build();
    }

    public Lesson isLessonExistById(Long id){

        return lessonRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException(String.format(ErrorMessages.NOT_FOUND_LESSON_MESSAGE, id)));
    }



    public ResponseMessage<LessonResponse> getLessonByName(String lessonName) {

        if(lessonRepository.getLessonByLessonNameIgnoreCase(lessonName).isPresent()){
            Lesson lesson = lessonRepository.getLessonByLessonNameIgnoreCase(lessonName).get();

            return ResponseMessage.<LessonResponse>builder()
                    .object(lessonMapper.mapLessonToLessonResponse(lesson))
                    .message(SuccessMessages.LESSON_FOUND)
                    .build();
        }else{

            return ResponseMessage.<LessonResponse>builder()
                    .message(String.format(ErrorMessages.NOT_FOUND_LESSON_MESSAGE,lessonName))
                    .build();
        }
    }



    public Page<LessonResponse> getAllLessonsByPage(int page, int size, String sort, String type) {

        Pageable pageable = pageableHelper.getPageableWithProperties(page,size,sort,type);

        return lessonRepository.findAll(pageable).map(lessonMapper::mapLessonToLessonResponse);
    }

    public Set<Lesson> getAllLessonsByLessonId(Set<Long> idSet) {

       return idSet.stream()
               .map(this::isLessonExistById)
               .collect(Collectors.toSet());

    }

    public ResponseMessage<LessonResponse> getLessonById(Long id) {

        Lesson lesson  = isLessonExistById(id);

        return ResponseMessage.<LessonResponse>builder()
                .object(lessonMapper.mapLessonToLessonResponse(lesson))
                .message(SuccessMessages.LESSON_FOUND)
                .build();





    }

    public LessonResponse updateLesson(Long lessonId, LessonRequest lessonRequest) {
        //does it exist
        Lesson lesson = isLessonExistById(lessonId);

        //if you change the name, does it exist in db?
        if(!lesson.getLessonName().equalsIgnoreCase(lessonRequest.getLessonName())
        &&lessonRepository.existsByLessonNameEqualsIgnoreCase(lessonRequest.getLessonName())){

            throw new ConflictException(String.format(ErrorMessages.ALREADY_REGISTER_LESSON_MESSAGE,lessonRequest.getLessonName()));
        }

        Lesson updatedLesson = lessonMapper.mapLessonRequestToLesson(lessonRequest);
        updatedLesson.setLessonId(lesson.getLessonId());
        //since we don't have LessonProgram in mapper
        updatedLesson.setLessonPrograms(lesson.getLessonPrograms());
        Lesson savedLesson = lessonRepository.save(updatedLesson);

        return lessonMapper.mapLessonToLessonResponse(savedLesson);

    }
}
