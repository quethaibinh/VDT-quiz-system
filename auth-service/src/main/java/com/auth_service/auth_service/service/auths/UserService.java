package com.auth_service.auth_service.service.auths;

import com.auth_service.auth_service.model.dto.auths.LoginDTO;
import com.auth_service.auth_service.model.dto.auths.RegisterDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import com.auth_service.auth_service.util.Checker;
import com.auth_service.auth_service.util.CryptoUtil;
import com.auth_service.auth_service.util.exception.ConflictException;
import com.auth_service.auth_service.util.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
/**
 * Xu ly dang nhap, dang ky va luu thong tin tai khoan.
 */
public class UserService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CryptoUtil cryptoUtil;
    @Autowired
    private Checker checker;

    /**
     * Xac thuc tai khoan dang hoat dong va phat JWT khi thong tin hop le.
     */
    public String login(LoginDTO userLogin) {

        UserEntity userEntity = userRepo.findByUsername(userLogin.getUsername());
        if (userEntity == null || !"ACTIVE".equals(userEntity.getStatus())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLogin.getUsername(), userLogin.getPassword()));
            if (authentication.isAuthenticated()) {
                return jwtService.generateToken(userEntity);
            }
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("Invalid username or password");
        }

        throw new UnauthorizedException("Invalid username or password");
    }

    /**
     * Kiem tra du lieu dang ky, ma hoa thong tin nhay cam va tao tai khoan.
     */
    public void register(RegisterDTO register) throws Exception {
        if (userRepo.existsByUsername(register.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepo.existsByStudentCode(register.getStudentCode())) {
            throw new ConflictException("Student code already exists");
        }
        if(register.getStudentCode() == null && register.getTeacherCode() == null){
            throw new IllegalArgumentException("invalid student code or teacher code");
        }
        if(!checker.emailChecker(register.getEmail())){
            throw new IllegalArgumentException("invalid Email");
        }
        if(!checker.passwordCheck(register.getPassword())){
            throw new IllegalArgumentException("The password isn't strong enough");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(register.getUsername());
        userEntity.setPasswordHash(passwordEncoder.encode(register.getPassword()));
        userEntity.setFullName(register.getFullName());
        userEntity.setEmail(register.getEmail());
        userEntity.setDisplayName(register.getFullName());
        userEntity.setPhoneEncrypt(cryptoUtil.encrypt(register.getPhone()));
        userEntity.setPhoneHash(cryptoUtil.generateBlindIndex(register.getPhone()));
        userEntity.setNationalIdEncrypt(cryptoUtil.encrypt(register.getNationalId()));
        userEntity.setNationalIdHash(cryptoUtil.generateBlindIndex(register.getNationalId()));
        userEntity.setBirthDate(register.getBirthDate());
        userEntity.setGender(register.getGender());
        userEntity.setStatus("ACTIVE");
        userEntity.setUserType(UserType.STUDENT);

        if(register.getStudentCode() != null){
            userEntity.setStudentCode(register.getStudentCode());
            userEntity.setUserType(UserType.STUDENT);
        }
        if(register.getTeacherCode() != null){
            userEntity.setTeacherCode(register.getTeacherCode());
            userEntity.setUserType(UserType.TEACHER);
        }

        userRepo.save(userEntity);
    }

}
