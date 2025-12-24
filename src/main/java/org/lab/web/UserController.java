package org.lab.web;

import org.lab.domain.User;
import org.lab.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public record CreateUserRequest(String login, String name) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody CreateUserRequest request) {
        return userService.register(request.login(), request.name());
    }

    @GetMapping
    public List<User> getAll() {
        return userService.getAll();
    }

    @GetMapping("/{login}")
    public User getByLogin(@PathVariable String login) {
        return userService.getByLogin(login);
    }
}


