package com.slicelending.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import com.slicelending.identity.domain.User;
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
