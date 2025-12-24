package org.lab.domain;

public record User(String login, String name) {
    public User {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login is empty");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is empty");
        }
    }
}
