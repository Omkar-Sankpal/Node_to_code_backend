package com.nodetocode.nodetocode_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NodetocodeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NodetocodeBackendApplication.class, args);
	}
}
