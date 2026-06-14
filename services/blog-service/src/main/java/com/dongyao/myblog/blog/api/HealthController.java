package com.dongyao.myblog.blog.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog")
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("service", "blog-service", "status", "UP");
    }
}
