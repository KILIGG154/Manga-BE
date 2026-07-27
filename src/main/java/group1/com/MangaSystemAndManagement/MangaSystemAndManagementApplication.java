package group1.com.MangaSystemAndManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
// Decision Log 2026-07-27 §AI-08: enable Spring @Scheduled jobs (chapter auto-publish).
@EnableScheduling
public class MangaSystemAndManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(MangaSystemAndManagementApplication.class, args);
	}

}
