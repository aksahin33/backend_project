package com.project.schoolmanagment.repository.business;

import com.project.schoolmanagment.entity.concretes.business.EducationTerm;
import com.project.schoolmanagment.entity.enums.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EducationTermRepository extends JpaRepository<EducationTerm, Long> {

    @Query("SELECT (count (e) > 0) FROM EducationTerm e WHERE e.term= ?1 AND extract(year from e.startDate) = ?2")
    boolean existsByTermAndYear(Term term, int year);


}
