package org.lab.service;

import org.lab.domain.DomainException;
import org.lab.domain.User;
import org.lab.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User register(String login, String name) {
        if (login == null || login.isBlank()) {
            throw new DomainException("login must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("name must not be blank");
        }
        if (userRepo.existsByLogin(login)) {
            throw new DomainException("User already exists: " + login);
        }
        User user = new User(login, name);
        userRepo.save(user);
        return user;
    }

    public User getByLogin(String login) {
        return userRepo.findByLogin(login)
                .orElseThrow(() -> new DomainException("Unknown user: " + login));
    }

    public boolean exists(String login) {
        return userRepo.existsByLogin(login);
    }

    public List<User> getAll() {
        return userRepo.findAll();
    }

    public void requireExists(String login) {
        if (!exists(login)) {
            throw new DomainException("Unknown user: " + login);
        }
    }
}


