package com.blockfoliox.crypto.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class TestController {

    @GetMapping("/test")
    public Map<String, String> test() {
        Map<String, String> map = new HashMap<>();
        map.put("status", "Backend is Working");
        map.put("project", "Crypto Tracker");
        return map;
    }
}

