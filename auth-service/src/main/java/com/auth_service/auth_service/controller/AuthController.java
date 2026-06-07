package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.auths.LoginDTO;
import com.auth_service.auth_service.model.dto.auths.RegisterDTO;
import com.auth_service.auth_service.service.auths.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/auth-service")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public String login(@RequestBody LoginDTO userLogin){
        return userService.login(userLogin);
    }

    @PostMapping("/register")
    public void register(@RequestBody RegisterDTO register) throws Exception {
        userService.register(register);
    }

    @PostMapping("/admin/register")
    public void adminRegister(){

    }

}
