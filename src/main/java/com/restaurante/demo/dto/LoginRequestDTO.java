package com.restaurante.demo.dto;

import lombok.Data;


//Los DTO son clases que nos permiten comunicarnos de manera más limpia con nuestra DB
//En este caso este DTO nos permite conseguir el usuario y contrasenia

@Data
public class LoginRequestDTO {
    private String username;
    private String password;
}