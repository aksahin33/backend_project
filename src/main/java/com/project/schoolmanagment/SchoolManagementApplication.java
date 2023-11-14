package com.project.schoolmanagment;
import com.project.schoolmanagment.entity.concretes.user.User;
import com.project.schoolmanagment.entity.concretes.user.UserRole;
import com.project.schoolmanagment.entity.enums.Gender;
import com.project.schoolmanagment.entity.enums.RoleType;
import com.project.schoolmanagment.repository.user.UserRepository;
import com.project.schoolmanagment.repository.user.UserRoleRepository;
import com.project.schoolmanagment.service.user.UserRoleService;
import com.project.schoolmanagment.service.user.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SchoolManagementApplication implements CommandLineRunner { //adds a run method which runs before the main method

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;

    private final PasswordEncoder passwordEncoder;

    public SchoolManagementApplication(UserRoleRepository userRoleRepository, UserRepository userRepository, UserRoleService userRoleService, PasswordEncoder passwordEncoder) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.passwordEncoder = passwordEncoder;
    }

    public static void main(String[] args) {
        SpringApplication.run(SchoolManagementApplication.class, args);
        //****---------
        //*****-------


    }

    @Override
    public void run(String... args) throws Exception {

        if(userRoleService.getAllUserRole().isEmpty()){
            UserRole admin = new UserRole();
            admin.setRoleName("Admin");
            admin.setRoleType(RoleType.ADMIN);
            userRoleRepository.save(admin);

            UserRole dean = new UserRole();
            dean.setRoleName("Dean");
            dean.setRoleType(RoleType.MANAGER);
            userRoleRepository.save(dean);

            UserRole viceDean = new UserRole();
            viceDean.setRoleName("ViceDean");
            viceDean.setRoleType(RoleType.ASSISTANT_MANAGER);
            userRoleRepository.save(viceDean);

            UserRole teacher = new UserRole();
            teacher.setRoleName("Teacher");
            teacher.setRoleType(RoleType.TEACHER);
            userRoleRepository.save(teacher);

            UserRole student = new UserRole();
            student.setRoleName("Student");
            student.setRoleType(RoleType.STUDENT);
            userRoleRepository.save(student);
        }

        if(!userRepository.existsByUsername("superAdmin")){
            User superAdmin = new User();
            superAdmin.setUsername("superAdmin");
            superAdmin.setEmail("admin@gmail.com");
            superAdmin.setSsn("111-11-1111");
            superAdmin.setPassword(passwordEncoder.encode("12345678"));
            superAdmin.setName("Michael");
            superAdmin.setSurname("Fraser");
            superAdmin.setPhoneNumber("111-111-1111");
            superAdmin.setGender(Gender.FEMALE);
            superAdmin.setBirthDay(LocalDate.of(1990,11,11));
            superAdmin.setBirthPlace("Alanya");
            superAdmin.setBuiltIn(true);
            superAdmin.setUserRole(userRoleService.getUserRole(RoleType.ADMIN));
            userRepository.save(superAdmin);
        }


    }
}









