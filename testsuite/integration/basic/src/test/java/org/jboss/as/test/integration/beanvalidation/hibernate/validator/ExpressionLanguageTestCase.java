/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that Unified EL expressions can be used in Bean Violation messages as supported since BV 1.1.
 *
 * @author Gunnar Morling
 */
@RunWith(Arquillian.class)
public class ExpressionLanguageTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "expression-language-validation.war");
        war.addClass(ExpressionLanguageTestCase.class);
        war.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");

        return war;
    }

    @Test
    public void testValidationUsingExpressionLanguage() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<TestBean>> violations = validator.validate(new TestBean());

        assertEquals(1, violations.size());
        assertEquals("'Bob' is too short, it should at least be 5 characters long.", violations.iterator().next().getMessage());
    }

    private static class TestBean {

        @Size(min = 5, message = "'${validatedValue}' is too short, it should at least be {min} characters long.")
        private final String name = "Bob";
    }
}
