package com.silvaldeweb.repository.address;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.address.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);

    List<Address> findByUserIdAndIsDefaultTrue(Long userId);
}
