package com.htc.dgft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DgftOrmMasterRequest {

    @NotBlank(message = "ORM Number is mandatory")
    @Size(max = 50, message = "ORM Number cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "ORM Number must be alphanumeric")
    private String ormNumber;

    @NotNull(message = "ORM Amount is mandatory")
    @DecimalMin(value = "1.00", message = "ORM Amount must be greater than 1")
    private BigDecimal ormAmount;

    @NotNull(message = "ORM Date is mandatory")
    @PastOrPresent(message = "ORM Date cannot be in the future")
    private LocalDate ormDate;

    @NotBlank(message = "AD Code is mandatory")
    @Size(min = 7, max = 7, message = "AD Code must be exactly 7 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "AD Code must be alphanumeric")
    private String adCode;

    @NotBlank(message = "Currency is mandatory")
    @Size(max = 3, message = "Currency cannot exceed 3 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Currency must contain alphabets only")
    private String ormCurrency;

    @NotBlank(message = "IFSC Code is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9]{11}$", message = "IFSC Code must be 11 alphanumeric characters")
    private String ifscCode;

    @Size(max = 10, message = "IE Code cannot exceed 10 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "IE Code must be alphanumeric")
    private String ieCode;

    @NotBlank(message = "Beneficiary Name is mandatory")
    @Size(max = 200, message = "Beneficiary Name cannot exceed 200 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_/\\\\.,;:*!#$@+^?\\s]+$", message = "Beneficiary Name contains invalid characters")
    private String beneficiaryName;

    @NotBlank(message = "Beneficiary Country is mandatory")
    @Size(max = 20, message = "Beneficiary Country cannot exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Beneficiary Country must contain alphabets only")
    private String beneficiaryCountry;

    @NotBlank(message = "Purpose Code is mandatory")
    @Size(max = 10, message = "Purpose Code cannot exceed 10 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Purpose Code must be alphanumeric")
    private String purposeCode;

    @Size(max = 25, message = "Bank Account Number cannot exceed 25 characters")
    @Pattern(regexp = "^[a-zA-Z0-9-]*$", message = "Bank Account Number allows only alphanumeric and hyphens")
    private String bankAccountNumber;

    @Size(max = 50, message = "Reference IRM cannot exceed 50 characters")
    private String referenceIrm;

    @PastOrPresent(message = "ORM Issue Date cannot be in the future")
    private LocalDate ormIssueDate;

    @NotBlank(message = "PAN Number is mandatory")
    @Pattern(regexp = "^[a-zA-Z0-9]{10}$", message = "PAN Number must be 10 alphanumeric characters")
    private String panNumber;

    @DecimalMin(value = "0.00", message = "INR Payable Amount must be positive")
    private BigDecimal inrPayableAmount;
}
