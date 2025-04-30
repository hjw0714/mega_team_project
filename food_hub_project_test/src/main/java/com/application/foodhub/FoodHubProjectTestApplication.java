package com.application.foodhub;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class FoodHubProjectTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(FoodHubProjectTestApplication.class, args);
	}
	
	
	@PostConstruct
    public void setTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
    
}
