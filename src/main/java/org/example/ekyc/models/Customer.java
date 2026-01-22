package org.example.ekyc.models;

import java.time.LocalDate;

import static org.example.util.Utils.isBlank;

public record Customer(
        String customerId,
        String fullName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        String address,
        String nationality
) {
        public Customer{
            if(isBlank(customerId)){
                throw new IllegalArgumentException("Customer ID is required");
            }

            if(isBlank(fullName)){
                throw new IllegalArgumentException("Full name is required");
            }

            if(dateOfBirth == null){
                throw new IllegalArgumentException("Date of Birth is required");
            }

            if(isBlank(address)){
                throw new IllegalArgumentException("Address is required");
            }
        }
}
