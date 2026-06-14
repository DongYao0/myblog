package com.dongyao.myblog.blog.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blog/ai/tasks")
public class AiTaskController {
    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/summary")
    public AiTask submitSummary(@RequestBody SummaryTaskRequest request) {
        return aiTaskService.submitSummary(request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AiTask> get(@PathVariable("id") Long id) {
        return aiTaskService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/result")
    public AiTask complete(@RequestBody AiTaskResultRequest request) {
        return aiTaskService.complete(request);
    }
}
