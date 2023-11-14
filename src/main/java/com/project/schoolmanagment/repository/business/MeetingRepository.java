package com.project.schoolmanagment.repository.business;

import com.project.schoolmanagment.entity.concretes.business.Meet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meet, Long> {

    List<Meet> findByStudentList_IdEquals(Long studentId);

    @Query("Select m from Meet m where m.advisoryTeacher.id = ?1")
    List<Meet> findByAdvisoryTeacher_IdEquals(Long teacherId);

    Page<Meet> findByAdvisoryTeacher_IdEquals(Long userId, Pageable pageable);
}
