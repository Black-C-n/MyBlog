package com.example.myblog.service;

import com.example.myblog.entity.MyUser;
import com.example.myblog.form.RegisterForm;
import com.example.myblog.repository.MyUserRepository;
import com.example.myblog.util.DjangoPasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final MyUserRepository userRepository;

    public UserService(MyUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<MyUser> findById(Integer id) {
        return userRepository.findById(id);
    }

    public MyUser getRequired(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在：" + id));
    }

    public Optional<MyUser> firstUser() {
        return userRepository.findFirstByOrderByIdAsc();
    }

    public List<MyUser> findAll() {
        return userRepository.findAll();
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username == null ? "" : username.trim());
    }

    @Transactional
    public Optional<MyUser> authenticate(String username, String rawPassword) {
        Optional<MyUser> result = userRepository.findByUsername(username == null ? "" : username.trim())
                .filter(MyUser::isActive)
                .filter(user -> DjangoPasswordUtil.matches(rawPassword, user.getPassword()));
        result.ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
        return result;
    }

    @Transactional
    public MyUser register(RegisterForm form) {
        String username = form.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户已存在");
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }

        MyUser user = new MyUser();
        user.setUsername(username);
        user.setPassword(DjangoPasswordUtil.encode(form.getPassword()));
        user.setSuperuser(true);
        user.setStaff(true);
        user.setActive(true);
        user.setDateJoined(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public MyUser saveProfile(MyUser user, String newPassword) {
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(DjangoPasswordUtil.encode(newPassword));
        }
        return userRepository.save(user);
    }
}
