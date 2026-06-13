package com.silvaldeweb.controller.dev;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.service.dev.DevSeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevSeedController {

    private static final Logger log = LoggerFactory.getLogger(DevSeedController.class);

    private final DevSeedService devSeedService;

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        log.info("POST /api/dev/seed invoked");
        return devSeedService.seed();
    }
}
