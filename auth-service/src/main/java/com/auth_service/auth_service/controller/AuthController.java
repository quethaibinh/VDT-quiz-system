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
/**
 * Cung cap API dang nhap va dang ky tai khoan.
 */
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    /**
     * Xac thuc thong tin dang nhap va tra ve JWT.
     */
    public String login(@RequestBody LoginDTO userLogin){
        return userService.login(userLogin);
    }

    @PostMapping("/register")
    /**
     * Dang ky tai khoan moi sau khi kiem tra du lieu va ma hoa thong tin nhay cam.
     */
    public void register(@RequestBody RegisterDTO register) throws Exception {
        userService.register(register);
    }

}
