package com.unla.epicentrogourmet.repositories;

import com.unla.epicentrogourmet.entities.Role;
import com.unla.epicentrogourmet.entities.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("roleRepository")
public interface IRoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRole(RoleName role);
}