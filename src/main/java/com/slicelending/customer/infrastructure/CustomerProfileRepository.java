package com.slicelending.customer.infrastructure;

import com.slicelending.customer.domain.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

}
