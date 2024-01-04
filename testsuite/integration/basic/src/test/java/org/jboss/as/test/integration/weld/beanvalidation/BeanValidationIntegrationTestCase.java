/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.beanvalidation;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.naming.InitialContext;
import jakarta.inject.Inject;
import javax.naming.NamingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the integration of CDI and Jakarta Bean Validation.
 *
 * @author Farah Juma
 */
@RunWith(Arquillian.class)
public class BeanValidationIntegrationTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "BeanValidationIntegrationTestCase.war");
        war.addPackage(BeanValidationIntegrationTestCase.class.getPackage());
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        war.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");
        return war;
    }

    @Inject
    ValidatorFactory defaultValidatorFactory;

    @Test
    public void testInjectedValidatorFactoryIsCdiEnabled() {
        assertNotNull(defaultValidatorFactory);
        Set<ConstraintViolation<Reservation>> violations = defaultValidatorFactory.getValidator().validate(new Reservation(1, "Smith"));
        assertEquals(1, violations.size());
        assertEquals("Not enough people for a reservation", violations.iterator().next().getMessage());
    }

    @Test
    public void testJndiBoundValidatorFactoryIsCdiEnabled() throws NamingException {
        ValidatorFactory validatorFactory = (ValidatorFactory) new InitialContext().lookup("java:comp/ValidatorFactory");
        assertNotNull(validatorFactory);

        Set<ConstraintViolation<Reservation>> violations = validatorFactory.getValidator().validate(new Reservation(4, null));
        List<String> actualViolations = new ArrayList<String>();
        for (ConstraintViolation<?> violation : violations) {
            actualViolations.add(violation.getMessage());
        }

        List<String> expectedViolations = new ArrayList<String>();
        expectedViolations.add("may not be null");
        expectedViolations.add("Not enough people for a reservation");

        Collections.sort(actualViolations);
        Collections.sort(expectedViolations);
        assertEquals(expectedViolations, actualViolations);
    }
}
