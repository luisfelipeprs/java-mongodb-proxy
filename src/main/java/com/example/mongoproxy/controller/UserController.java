package com.example.mongoproxy.controller;

import com.example.mongoproxy.model.User;
import com.example.mongoproxy.service.UserService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.List;

@Controller("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /users -> lista todos os usuários da coleção configurada
    @Get
    public List<User> list() {
        return userService.listUsers();
    }
}
