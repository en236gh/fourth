package com.backend.fourth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FourthApplication {

	public static void main(String[] args) {
		SpringApplication.run(FourthApplication.class, args);
	}

}
