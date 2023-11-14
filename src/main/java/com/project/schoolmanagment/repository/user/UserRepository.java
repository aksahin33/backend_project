package com.project.schoolmanagment.repository.user;

import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.entity.enums.RoleType;
import com.project.schoolmanagment.payload.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsBySsn(String ssn);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.userRole.roleName = :userRole")
    Page<User> findUserByRole(String userRole, Pageable pageable);


    List<User> getUserByNameContaining(String userName);

    User findByUsername(String username);


    @Query("SELECT u from User u where u.isAdvisor = ?1")
    List<User> findAllByAdvisorTeacher(boolean isAdvisor);



    List<User> findByAdvisorTeacherId(Long id);

    @Query(value = "select (count (u)>0) from User u where u.userRole.roleType = ?1")
    boolean findUsersByRoleType(RoleType roleType);

    @Query("Select max (u.studentNumber) from User u")
    int getMaxStudentNumber();


    @Query("Select u from User u where u.id in :userIds")
    List<User> findUsersByIdArray(Long[] userIds);




























}
