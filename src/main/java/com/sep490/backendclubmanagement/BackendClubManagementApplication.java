package com.sep490.backendclubmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendClubManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendClubManagementApplication.class, args);
    }

}
