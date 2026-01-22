package org.example.ekyc.validation;

import org.example.ekyc.exception.ValidationException;
import org.example.ekyc.models.Customer;

import java.util.regex.Pattern;

import static org.example.util.Utils.isBlank;

public class CustomerValidator {
    private CustomerValidator() {}

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9\\-\\s]{6,}$");

    public static void validate(Customer customer) {
        if(customer == null) {
            throw new ValidationException("Customer is required");
        }

        if(isBlank(customer.customerId())) {
            throw new ValidationException("Customer ID is required");
        }

        if(isBlank(customer.fullName())) {
            throw new ValidationException("Customer full name is required");
        }

        if(customer.dateOfBirth() == null) {
            throw new ValidationException("Customer date of birth is required");
        }

        if(isBlank(customer.address())) {
            throw new ValidationException("Customer address is required");
        }

        if(!isBlank(customer.email()) && !EMAIL_PATTERN.matcher(customer.email()).matches()) {
            throw new ValidationException("Customer email is invalid");
        }

        if(isBlank(customer.phone()) && !PHONE_PATTERN.matcher(customer.phone()).matches()) {
            throw new ValidationException("Customer phone is invalid");
        }
    }



}
