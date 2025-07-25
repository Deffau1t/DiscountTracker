package com.example.controller;


import com.example.dto.RegistrationForm;
import com.example.entity.User;
import com.example.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@AllArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("registrationForm") @Valid RegistrationForm form,
                                      BindingResult result,
                                      Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "Пароли не совпадают");
            return "register";
        }

        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            model.addAttribute("error", "Email уже зарегистрирован");
            return "register";
        }

        User user = new User();
        user.setEmail(form.getEmail());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userRepository.save(user);

        return "redirect:/login";
    }
}