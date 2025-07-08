package com.ebanking.controller;

import com.ebanking.dto.TokenRequest;
import com.ebanking.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    @Autowired
    private JwtService jwtService;

    @PostMapping("/generate")
    public Map<String, String> generateToken(@RequestBody TokenRequest request) {
        String token = jwtService.generateToken(request.getCustomerId());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return response;
    }
}