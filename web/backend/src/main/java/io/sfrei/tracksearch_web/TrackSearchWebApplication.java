package io.sfrei.tracksearch_web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrackSearchWebApplication {

	public static void main(String[] args) {
		System.err.close();
		System.setErr(System.out);
		SpringApplication.run(TrackSearchWebApplication.class, args);
	}

}
