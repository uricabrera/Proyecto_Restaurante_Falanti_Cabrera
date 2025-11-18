package com.restaurante.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
	
	// Redirecciona solamente a index.html porque es ahí donde index.html luego se encarga de direccionar el resto de la aplicacion

    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    @GetMapping("/home")
    public String home() {
        return "redirect:/index.html";
    }
}