package com.project.schoolmanagment.service.business;

import com.project.schoolmanagment.entity.concretes.business.EducationTerm;
import com.project.schoolmanagment.exception.BadRequestException;
import com.project.schoolmanagment.exception.ConflictException;
import com.project.schoolmanagment.exception.ResourceNotFoundException;
import com.project.schoolmanagment.payload.mappers.EducationTermMapper;
import com.project.schoolmanagment.payload.messages.ErrorMessages;
import com.project.schoolmanagment.payload.messages.SuccessMessages;
import com.project.schoolmanagment.payload.request.business.EducationTermRequest;
import com.project.schoolmanagment.payload.response.abstracts.ResponseMessage;
import com.project.schoolmanagment.payload.response.business.EducationTermResponse;
import com.project.schoolmanagment.repository.business.EducationTermRepository;
import com.project.schoolmanagment.service.helper.PageableHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducationTermService {


    private final EducationTermRepository educationTermRepository;
    private final EducationTermMapper educationTermMapper;
    private final PageableHelper pageableHelper;


    public ResponseMessage<EducationTermResponse> saveEducationTerm(EducationTermRequest educationTermRequest) {
        validateEducationTerms(educationTermRequest);

        EducationTerm savedEducationTerm = educationTermRepository
                .save(educationTermMapper.mapEducationTermRequestToEducationTerm(educationTermRequest));

        return ResponseMessage.<EducationTermResponse>builder()
                .message(SuccessMessages.EDUCATION_TERM_SAVE)
                .object(educationTermMapper.mapEducationTermToEducationTermResponse(savedEducationTerm))
                .httpStatus(HttpStatus.CREATED)
                .build();


    }


    private void validateEducationTermDatesForRequest(EducationTermRequest request){

        //registration > start
        if(request.getLastRegistrationDate().isAfter(request.getStartDate())){
            throw new BadRequestException(ErrorMessages.EDUCATION_START_DATE_IS_EARLIER_THAN_LAST_REGISTRATION_DATE);
        }


        //end > start
        if(request.getEndDate().isBefore(request.getStartDate())) {
            throw new ConflictException(ErrorMessages.EDUCATION_END_DATE_IS_EARLIER_THAN_START_DATE);

        }
    }


    private void validateEducationTerms(EducationTermRequest request){

        validateEducationTermDatesForRequest(request);

        //only one education term (fall or spring) can exist in a year
        if(educationTermRepository.existsByTermAndYear(request.getTerm(),request.getStartDate().getYear())){
            throw new ResourceNotFoundException(ErrorMessages.EDUCATION_TERM_IS_ALREADY_EXIST_BY_TERM_AND_YEAR_MESSAGE);
        }

        //TODO
        //while we create an education term there should not be any conflict between the education terms
    }

    public List<EducationTermResponse> getAllEducationTerms() {

        return educationTermRepository.findAll()
                .stream()
                .map(educationTermMapper::mapEducationTermToEducationTermResponse)
                .collect(Collectors.toList());

    }

    public EducationTermResponse findEducationTermById(Long id) {

        EducationTerm educationTerm = isEducationTermExist(id);

        return educationTermMapper.mapEducationTermToEducationTermResponse(educationTerm);

    }

    public EducationTerm isEducationTermExist(Long id){

        return educationTermRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException(String.format(ErrorMessages.EDUCATION_TERM_NOT_FOUND_MESSAGE, id)));

    }

    public ResponseMessage<EducationTermResponse> updateEducationTerm(Long id, EducationTermRequest educationTermRequest) {
        //does it exist?
        isEducationTermExist(id);

        //check the expected dates are correct
        validateEducationTermDatesForRequest(educationTermRequest);

        EducationTerm updatedEducationTerm = educationTermRepository.save(
                educationTermMapper.mapEducationTermRequestToEducationTermForUpdate(id, educationTermRequest));



        return ResponseMessage.<EducationTermResponse>builder()
                .message(SuccessMessages.EDUCATION_TERM_UPDATE)
                .object(educationTermMapper.mapEducationTermToEducationTermResponse(updatedEducationTerm))
                .httpStatus(HttpStatus.OK)
                .build();


    }

    public ResponseMessage deleteById(Long id) {

        isEducationTermExist(id);
        educationTermRepository.deleteById(id);
        return ResponseMessage.builder()
                .message(SuccessMessages.EDUCATION_TERM_DELETE)
                .httpStatus(HttpStatus.OK)
                .build();

    }

    public Page<EducationTermResponse> getAllEducationTermsByPage(int page, int size, String sort, String type) {

        Pageable pageable = pageableHelper.getPageableWithProperties(page,size,sort,type);

        return educationTermRepository.findAll(pageable)
                .map(educationTermMapper::mapEducationTermToEducationTermResponse);

    }
}
