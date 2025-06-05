package br.com.vpnmanager.controller;

import br.com.vpnmanager.entity.User;
import br.com.vpnmanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUsers(Model model, @RequestParam(value = "search", required = false) String search) {
        List<User> users = (search == null || search.isEmpty()) ? userService.findAll()
                : userService.searchByUsername(search);
        model.addAttribute("users", users);
        return "user/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        return "user/create";
    }

    @PostMapping("/save")
    public String createUser(User user) {
        userService.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        return "user/edit";
    }

    @PostMapping("/update")
    public String updateUser(User user) {
        userService.update(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/admin/users";
    }
}
