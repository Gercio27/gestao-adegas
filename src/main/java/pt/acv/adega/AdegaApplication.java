package pt.acv.adega;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Software de gestao de pequenas adegas - ACV Vinhos de Talha.
 * Da vinha ate a rotulagem / produto acabado.
 */
@SpringBootApplication
public class AdegaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdegaApplication.class, args);
    }
}
