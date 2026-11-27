/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that message interpolation works correctly for Hibernate Validator.
 *
 * @author Madhumita Sadhukhan
 */
@ExtendWith(ArquillianExtension.class)
public class MessageInterpolationValidationTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testmessageinterpolationvalidation.war");
        war.addPackage(MessageInterpolationValidationTestCase.class.getPackage());
        war.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");
        return war;
    }

    @Test
    public void testCustomMessageInterpolation() {

        HibernateValidatorConfiguration configuration = Validation.byProvider(HibernateValidator.class).configure();
        assertNotNull(configuration);

        final MessageInterpolator messageInterpolator = new CustomMessageInterpolator();
        configuration.messageInterpolator(messageInterpolator);

        ValidatorFactory factory = configuration.buildValidatorFactory();
        Validator validator = factory.getValidator();

        // create employee
        Employee emp = new Employee();
        emp.setEmail("MADHUMITA");

        Set<ConstraintViolation<Employee>> constraintViolations = validator.validate(emp);

        assertEquals(constraintViolations.size(), 1, "Wrong number of constraints");
        assertEquals(CustomMessageInterpolator.MESSAGE, constraintViolations.iterator().next().getMessage());
    }

    private static class CustomMessageInterpolator implements MessageInterpolator {

        public static final String MESSAGE = "Message created by custom interpolator";

        @Override
        public String interpolate(String messageTemplate, Context context) {
            return MESSAGE;
        }

        @Override
        public String interpolate(String messageTemplate, Context context, Locale locale) {
            return MESSAGE;
        }
    }
}
