package com.blockfoliox.crypto.dto;

public class UserDTO {

    private String name;
    private String email;
    private String password;

    // EMPTY constructor (Spring needs this)
    public UserDTO() {}

    // PARAMETER constructor (for manual mapping)
    public UserDTO(Long id, String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
