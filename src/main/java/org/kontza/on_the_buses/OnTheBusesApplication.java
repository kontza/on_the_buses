package org.kontza.on_the_buses;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

@SpringBootApplication
@Slf4j
public class OnTheBusesApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnTheBusesApplication.class, args);
    }
}
