package prototype.coreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Core API Spring Boot application.
 * This application provides the backend services for the RAG Chatbot Prototype,
 * including authentication, document management, and chat functionalities.
 */
@EnableAsync
@SpringBootApplication
public class CoreApiApplication {

    /**
     * Runs the Spring Boot application.
     * 
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(CoreApiApplication.class, args);
    }

}
