package com.deushu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class DeuShuApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeuShuApplication.class, args);
	}

}
