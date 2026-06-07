package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserGender;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import com.auth_service.auth_service.util.CryptoUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ImportedUserCreationService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CryptoUtil cryptoUtil;

    public ImportedUserCreationService(
            UserRepo userRepo,
            PasswordEncoder passwordEncoder,
            CryptoUtil cryptoUtil
    ) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.cryptoUtil = cryptoUtil;
    }

    public void createUser(ImportUserRowDTO row) throws Exception {
        // Ma sinh vien/giao vien duoc dung lam ca username va mat khau mac dinh.
        String code = !isBlank(row.getStudentCode()) ? row.getStudentCode() : row.getTeacherCode();

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(code);
        userEntity.setPasswordHash(passwordEncoder.encode(code));
        userEntity.setFullName(row.getFullName());
        userEntity.setEmail(row.getEmail());
        userEntity.setDisplayName(row.getFullName());
        userEntity.setStatus("ACTIVE");

        if (!isBlank(row.getStudentCode())) {
            userEntity.setStudentCode(row.getStudentCode());
            userEntity.setUserType(UserType.STUDENT);
        } else {
            userEntity.setTeacherCode(row.getTeacherCode());
            userEntity.setUserType(UserType.TEACHER);
        }

        if (!isBlank(row.getPhone())) {
            userEntity.setPhoneEncrypt(cryptoUtil.encrypt(row.getPhone()));
            userEntity.setPhoneHash(cryptoUtil.generateBlindIndex(row.getPhone()));
        }

        if (!isBlank(row.getNationalId())) {
            userEntity.setNationalIdEncrypt(cryptoUtil.encrypt(row.getNationalId()));
            userEntity.setNationalIdHash(cryptoUtil.generateBlindIndex(row.getNationalId()));
        }

        if (!isBlank(row.getBirthDate())) {
            userEntity.setBirthDate(LocalDate.parse(row.getBirthDate()));
        }

        if (!isBlank(row.getGender())) {
            userEntity.setGender(UserGender.valueOf(row.getGender().toUpperCase()));
        }

        userRepo.save(userEntity);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
