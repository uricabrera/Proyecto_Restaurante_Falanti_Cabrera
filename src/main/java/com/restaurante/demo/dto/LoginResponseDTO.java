package com.restaurante.demo.dto;


//Los DTO son clases que nos permiten comunicarnos de manera más limpia con nuestra DB
//En este caso este DTO nos permite obtener un LoginResponse legible

public class LoginResponseDTO {
    private Long userId;
    private String role;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}