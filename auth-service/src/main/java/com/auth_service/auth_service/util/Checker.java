package com.auth_service.auth_service.util;

import org.springframework.stereotype.Component;

@Component
public class Checker {

    public boolean emailChecker(String email){
        if(!email.endsWith("@gmail.com")) return false;
        return true;
    }

    public boolean passwordCheck(String password){
        return true;
    }

}
