/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.beanvalidation;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests Jakarta Bean Validation within a Jakarta EE component
 * <p/>
 * User: Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class BeanValidationTestCase {

    @EJB(mappedName = "java:module/StatelessBean")
    private StatelessBean slsb;

    @Deployment
    public static JavaArchive getDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bean-validation-test.jar");
        jar.addClass(StatelessBean.class);

        return jar;
    }

    /**
     * Test that {@link jakarta.validation.Validation#buildDefaultValidatorFactory()} works fine within an EJB
     */
    @Test
    public void testBuildDefaultValidatorFactory() {
        this.slsb.buildDefaultValidatorFactory();
    }

    /**
     * Test that {@link jakarta.validation.Validator} and {@link jakarta.validation.ValidatorFactory} are injected in a
     * EJB
     */
    @Test
    public void testValidatorInjection() {
        Assertions.assertTrue(this.slsb.isValidatorInjected(), "Validator wasn't injected in EJB");
        Assertions.assertTrue(this.slsb.isValidatorFactoryInjected(), "ValidatorFactory wasn't injected in EJB");
    }

    /**
     * Tests that {@link jakarta.validation.ValidatorFactory#getValidator()} returns a validator
     */
    @Test
    public void testGetValidatorFromValidatorFactory() {
        Assertions.assertNotNull(this.slsb.getValidatorFromValidatorFactory(), "Validator from ValidatorFactory was null");
    }
}
