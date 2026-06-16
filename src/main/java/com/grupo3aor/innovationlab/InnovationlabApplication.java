package com.grupo3aor.innovationlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Esta é a classe principal da aplicação, o ponto de partida onde configuro o arranque do servidor Spring Boot.
 */
@SpringBootApplication
@EnableScheduling
public class InnovationlabApplication {

	public static void main(String[] args) {
		SpringApplication.run(InnovationlabApplication.class, args);
	}

}
