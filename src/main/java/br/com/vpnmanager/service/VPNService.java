package br.com.vpnmanager.service;

import br.com.vpnmanager.entity.User;
import br.com.vpnmanager.entity.VPN;
import br.com.vpnmanager.repository.VPNRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class VPNService {

    @Autowired
    private VPNRepository vpnRepository;

    @Autowired
    @Lazy
    private UserService userService;

    private static final SecureRandom random = new SecureRandom();

    private static final String CREATE_SCRIPT = "/opt/easy-rsa/create_certificate_script.py";
    private static final String REVOKE_SCRIPT = "/opt/easy-rsa/revoke_certificate_script.py";

    public List<VPN> findByUser(User user) {
        return vpnRepository.findByUser(user);
    }

    public List<VPN> searchByLabel(User user, String label) {
        return vpnRepository.findByUserAndLabelContainingIgnoreCase(user, label);
    }

    public void create(Long userId) {
        User user = userService.findById(userId);
        String label = generateUniqueLabel();

        try {
            Process process = new ProcessBuilder("python3",
                    CREATE_SCRIPT,
                    user.getUsername(),
                    label).start();
            process.waitFor();

            VPN vpn = new VPN();
            vpn.setLabel(label);
            vpn.setCreatedDate(LocalDateTime.now());
            vpn.setDownloadUrl("/exports/download/" + user.getUsername() + "_" + vpn.getLabel() + ".zip");
            vpn.setUser(user);
            vpnRepository.save(vpn);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to create certificate", e);
        }
    }

    public void revoke(Long vpnId) {
        try {
            VPN vpn = vpnRepository.findById(vpnId).orElseThrow();

            Process process = new ProcessBuilder("python3",
                    REVOKE_SCRIPT,
                    vpn.getUser().getUsername(),
                    vpn.getLabel()).start();
            process.waitFor();

            vpnRepository.deleteById(vpnId);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to revoke certificate", e);
        }
    }

    private String generateUniqueLabel() {
        byte[] bytes = new byte[5]; // ~7 base64 characters
        String label;
        do {
            random.nextBytes(bytes);
            label = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (vpnRepository.existsByLabel(label));
        return label;
    }

    public boolean belongsToUser(Long vpnId, User user) {
        return vpnRepository.findById(vpnId)
                .map(vpn -> vpn.getUser().getId().equals(user.getId()))
                .orElse(false);
    }
}
