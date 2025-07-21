package com.example.ainovel.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @Value("${spa.redirect.url}")
    private String redirectUrl;

    @RequestMapping(value = {"/", "/{path:[^\\.]*}"})
    public String redirect() {
        return redirectUrl;
    }
}
