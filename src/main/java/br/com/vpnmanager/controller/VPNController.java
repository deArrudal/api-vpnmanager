package br.com.vpnmanager.controller;

import br.com.vpnmanager.entity.User;
import br.com.vpnmanager.entity.VPN;
import br.com.vpnmanager.service.UserService;
import br.com.vpnmanager.service.VPNService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/users/{userId}/vpns")
public class VPNController {

    @Autowired
    private VPNService vpnService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listVpns(@PathVariable Long userId, @RequestParam(value = "search", required = false) String search,
            Model model) {
        User user = userService.findById(userId);
        List<VPN> vpns = (search == null || search.isEmpty()) ? vpnService.findByUser(user)
                : vpnService.searchByLabel(user, search);
        model.addAttribute("user", user);
        model.addAttribute("vpns", vpns);
        return "vpn/list";
    }

    @PostMapping("/{vpnId}/revoke")
    public String revokeVpn(@PathVariable Long userId, @PathVariable Long vpnId) {
        vpnService.revoke(vpnId);
        return "redirect:/admin/users/" + userId + "/vpns";
    }

    @GetMapping("/create")
    public String createVpn(@PathVariable Long userId) {
        vpnService.create(userId);
        return "redirect:/admin/users/" + userId + "/vpns";
    }
}
