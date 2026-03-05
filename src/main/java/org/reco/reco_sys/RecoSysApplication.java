package org.reco.reco_sys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecoSysApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecoSysApplication.class, args);
	}

}
