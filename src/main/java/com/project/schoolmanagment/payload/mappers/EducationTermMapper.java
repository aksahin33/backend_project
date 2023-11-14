package com.project.schoolmanagment.payload.mappers;

import com.project.schoolmanagment.entity.concretes.business.EducationTerm;
import com.project.schoolmanagment.payload.request.business.EducationTermRequest;
import com.project.schoolmanagment.payload.response.business.EducationTermResponse;
import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class EducationTermMapper {

    //DTO --> DomainObject mapper
    public EducationTerm mapEducationTermRequestToEducationTerm(EducationTermRequest request){

        return EducationTerm.builder()
                .term(request.getTerm())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .lastRegistrationDate(request.getLastRegistrationDate())
                .build();

    }


    //DomainObject --> DTO mapper
    public EducationTermResponse mapEducationTermToEducationTermResponse(EducationTerm educationTerm){

        return EducationTermResponse.builder()
                .id(educationTerm.getId())
                .term(educationTerm.getTerm())
                .startDate(educationTerm.getStartDate())
                .endDate(educationTerm.getEndDate())
                .lastRegistrationDate(educationTerm.getLastRegistrationDate())
                .build();

    }


    public EducationTerm mapEducationTermRequestToEducationTermForUpdate(Long id, EducationTermRequest request){

        return mapEducationTermRequestToEducationTerm(request)
                .toBuilder()
                .id(id)
                .build();

//        EducationTerm educationTerm = mapEducationTermRequestToEducationTerm(request);
//        educationTerm.setId(id);
//        return educationTerm;

    }




}
