package br.com.vpnmanager.repository;

import br.com.vpnmanager.entity.User;
import br.com.vpnmanager.entity.VPN;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VPNRepository extends JpaRepository<VPN, Long> {
    List<VPN> findByUser(User user);

    List<VPN> findByUserAndLabelContainingIgnoreCase(User user, String label);

    boolean existsByLabel(String label);
}
