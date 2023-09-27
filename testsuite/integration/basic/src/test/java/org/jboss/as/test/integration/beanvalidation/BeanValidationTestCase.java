/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.beanvalidation;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests Jakarta Bean Validation within a Jakarta EE component
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
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
        Assert.assertTrue("Validator wasn't injected in EJB", this.slsb.isValidatorInjected());
        Assert.assertTrue("ValidatorFactory wasn't injected in EJB", this.slsb.isValidatorFactoryInjected());
    }

    /**
     * Tests that {@link jakarta.validation.ValidatorFactory#getValidator()} returns a validator
     */
    @Test
    public void testGetValidatorFromValidatorFactory() {
        Assert.assertNotNull("Validator from ValidatorFactory was null", this.slsb.getValidatorFromValidatorFactory());
    }
}
