package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("userRepository")
public interface IUserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);
}