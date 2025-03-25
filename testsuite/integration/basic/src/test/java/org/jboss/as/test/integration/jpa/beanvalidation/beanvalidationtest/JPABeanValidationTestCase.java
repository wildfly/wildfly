/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.beanvalidation.beanvalidationtest;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.validation.ConstraintViolationException;

import org.hibernate.validator.HibernateValidatorPermission;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.jpa.beanvalidation.Employee;
import org.jboss.as.test.integration.jpa.beanvalidation.SFSB1;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Jakarta Bean Validation with Jakarta Persistence test
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class JPABeanValidationTestCase {

    private static final String ARCHIVE_NAME = "JPABeanValidationTestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(JPABeanValidationTestCase.class,
                Employee.class,
                SFSB1.class
        );
        jar.addAsManifestResource(JPABeanValidationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
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

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * Test that a Jakarta Bean Validation error is not thrown
     *
     * @throws Exception
     */
    @Test
    public void testSuccessfulBeanValidation() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("name", "address", 1);
    }

    /**
     * Test that a Jakarta Bean Validation error is thrown
     *
     * @throws Exception
     */
    @Test
    public void testFailingBeanValidationNullAddress() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        try {
            sfsb1.createEmployee("name", null, 2);
            fail("should of thrown validation error for null address in Employee entity");
        } catch (Throwable throwable) {
            ConstraintViolationException constraintViolationException = null;
            // find the ConstraintViolationException
            while (throwable != null && !(throwable instanceof ConstraintViolationException)) {
                throwable = throwable.getCause();
            }
            // should be null or instanceof ConstraintViolationException
            constraintViolationException = (ConstraintViolationException) throwable;
            assertTrue("expected ConstraintViolationException but got " + constraintViolationException, constraintViolationException instanceof ConstraintViolationException);
        }
    }

}
