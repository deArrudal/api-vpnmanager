package br.com.vpnmanager.controller;

import br.com.vpnmanager.entity.User;
import br.com.vpnmanager.entity.VPN;
import br.com.vpnmanager.service.UserService;
import br.com.vpnmanager.service.VPNService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/vpns")
public class UserController {

    @Autowired
    private VPNService vpnService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listUserVpns(Model model, Authentication auth,
            @RequestParam(value = "search", required = false) String search) {
        User user = userService.findByUsername(auth.getName());
        List<VPN> vpns = (search == null || search.isEmpty()) ? vpnService.findByUser(user)
                : vpnService.searchByLabel(user, search);
        model.addAttribute("user", user);
        model.addAttribute("vpns", vpns);
        return "vpn/list";
    }

    @PostMapping("/{vpnId}/revoke")
    public String revokeUserVpn(@PathVariable Long vpnId, Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        if (vpnService.belongsToUser(vpnId, user)) {
            vpnService.revoke(vpnId);
        }
        return "redirect:/vpns";
    }

    @GetMapping("/create")
    public String createUserVpn(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        vpnService.create(user.getId());
        return "redirect:/vpns";
    }
}
