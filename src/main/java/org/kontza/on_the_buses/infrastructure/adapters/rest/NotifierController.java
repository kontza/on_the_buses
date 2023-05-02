package org.kontza.on_the_buses.infrastructure.adapters.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("notify")
public class NotifierController {
    private static final Logger logger = LoggerFactory.getLogger(NotifierController.class);

    @GetMapping()
    public void notifier() {
        logger.error("Not supported yet!");
    }
}