/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.beanvalidation.cdi;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the integration of Jakarta Persistence, Jakarta Contexts and Dependency Injection, and Jakarta Bean Validation.
 *
 * @author Farah Juma
 */
@RunWith(Arquillian.class)
public class BeanValidationCdiIntegrationTestCase {

    private static final String ARCHIVE_NAME = "BeanValidationCdiIntegrationTestCase";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(BeanValidationCdiIntegrationTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
        jar.addAsManifestResource(BeanValidationCdiIntegrationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(createPermissionsXmlAsset(
                HibernateValidatorPermission.ACCESS_PRIVATE_MEMBERS
        ), "permissions.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testSuccessfulBeanValidation() throws Exception {
        SFSB sfsb = lookup("SFSB", SFSB.class);
        sfsb.createReservation(6, "Smith");
    }

    @Test
    public void testFailingBeanValidation() throws Exception {
        SFSB sfsb = lookup("SFSB", SFSB.class);
        try {
            sfsb.createReservation(1, null);
            fail("Should have thrown validation error for invalid values in Reservation entity");
        } catch (Throwable throwable) {
            ConstraintViolationException constraintViolationException = null;

            // Find the ConstraintViolationException
            while (throwable != null && !(throwable instanceof ConstraintViolationException)) {
                throwable = throwable.getCause();
            }

            constraintViolationException = (ConstraintViolationException) throwable;

            Set<ConstraintViolation<?>> violations = constraintViolationException.getConstraintViolations();
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
}
