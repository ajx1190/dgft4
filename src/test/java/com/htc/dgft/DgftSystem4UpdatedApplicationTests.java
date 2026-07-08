package com.htc.dgft;

import com.htc.dgft.dto.request.DgftOrmMasterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = DgftSystemApplication.class)
class DgftSystem4UpdatedApplicationTests {

	@Autowired
	private Validator validator;

	@Test
	void contextLoads() {
	}

	@Test
	void testValidDto() {
		DgftOrmMasterRequest request = new DgftOrmMasterRequest();
		request.setOrmNumber("ORM12345");
		request.setOrmAmount(new BigDecimal("100.00"));
		request.setOrmDate(LocalDate.now());
		request.setAdCode("1234567");
		request.setOrmCurrency("USD");
		request.setIfscCode("HDFC0001234");
		request.setBeneficiaryName("John Doe");
		request.setBeneficiaryCountry("USA");
		request.setPurposeCode("P0104");
		request.setPanNumber("ABCDE1234F");

		Set<ConstraintViolation<DgftOrmMasterRequest>> violations = validator.validate(request);
		assertTrue(violations.isEmpty(), "Valid request should have no violations");
	}

	@Test
	void testInvalidDto() {
		DgftOrmMasterRequest request = new DgftOrmMasterRequest();
		// Empty fields should fail validation
		Set<ConstraintViolation<DgftOrmMasterRequest>> violations = validator.validate(request);
		assertFalse(violations.isEmpty(), "Empty request should have validation errors");
	}

}
