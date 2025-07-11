package com.surgery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class SurgeryReservationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SurgeryReservationSystemApplication.class, args);
    }
} 