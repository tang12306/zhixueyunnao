package com._1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ai")
public class AIViewController {
    
    // @GetMapping("/question-generator") // Commented out to disable old AI question page
    // public String questionGenerator() {
    //     return "ai/question_generator";
    // }
} 