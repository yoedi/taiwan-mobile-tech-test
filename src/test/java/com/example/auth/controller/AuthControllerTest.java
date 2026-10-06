package com.example.auth.controller;

import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerSuccess() throws Exception {
        register("test@example.com", "mypassword")
                .andExpect(status().isCreated())
                .andExpect(content().string("User registered successfully."));
    }

    @Test
    void registerDuplicateEmailReturnsConflict() throws Exception {
        register("test@example.com", "mypassword");

        register("Test@Example.com", "mypassword")
                .andExpect(status().isConflict())
                .andExpect(content().string("Email already registered"));
    }

    @Test
    void registerInvalidEmailReturnsBadRequest() throws Exception {
        register("not-an-email", "mypassword")
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email format"));
    }

    @Test
    void registerBlankPasswordReturnsBadRequest() throws Exception {
        register("test@example.com", "")
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginSuccess() throws Exception {
        register("test@example.com", "mypassword");

        login("test@example.com", "mypassword")
                .andExpect(status().isOk())
                .andExpect(content().string("Login successful"));
    }

    @Test
    void loginWrongPasswordReturnsUnauthorized() throws Exception {
        register("test@example.com", "mypassword");

        login("test@example.com", "wrongpass")
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void loginUnknownEmailReturnsUnauthorized() throws Exception {
        login("nobody@example.com", "mypassword")
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    void loginMissingEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("password", "mypassword"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email is required"));
    }

    private ResultActions register(String email, String password) throws Exception {
        return submit("/api/register", email, password);
    }

    private ResultActions login(String email, String password) throws Exception {
        return submit("/api/login", email, password);
    }

    private ResultActions submit(String path, String email, String password) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", email)
                .param("password", password));
    }
}
