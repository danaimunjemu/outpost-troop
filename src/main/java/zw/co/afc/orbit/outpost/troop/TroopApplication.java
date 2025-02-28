package zw.co.afc.orbit.outpost.troop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TroopApplication {

	public static void main(String[] args) {
		SpringApplication.run(TroopApplication.class, args);
	}

}
