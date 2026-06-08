package com.silvaldeweb.repository.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByActive(Boolean active);

    List<User> findByRole(Role role);

    List<User> findByActiveAndRole(Boolean active, Role role);
}
