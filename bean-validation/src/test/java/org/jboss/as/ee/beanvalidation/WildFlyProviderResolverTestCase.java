/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.spi.ValidationProvider;

import org.hibernate.validator.HibernateValidator;
import org.jboss.as.ee.beanvalidation.testprovider.MyValidationProvider;
import org.jboss.as.ee.beanvalidation.testutil.ContextClassLoaderRule;
import org.jboss.as.ee.beanvalidation.testutil.WithContextClassLoader;
import org.jboss.as.ee.beanvalidation.testutil.WithContextClassLoader.NullClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for {@link WildFlyProviderResolver}.
 *
 * @author Gunnar Morling
 */
public class WildFlyProviderResolverTestCase {

    @Rule
    public final ContextClassLoaderRule contextClassLoaderRule = new ContextClassLoaderRule();

    private ValidationProviderResolver providerResolver;

    @Before
    public void setupProviderResolver() {
        providerResolver = new WildFlyProviderResolver();
    }

    @Test
    public void testHibernateValidatorIsFirstProviderInList() {
        List<ValidationProvider<?>> validationProviders = providerResolver.getValidationProviders();

        assertEquals(2, validationProviders.size());
        assertEquals(HibernateValidator.class.getName(), validationProviders.get(0).getClass().getName());
        assertEquals(MyValidationProvider.class.getName(), validationProviders.get(1).getClass().getName());
    }

    @Test
    @WithContextClassLoader(NullClassLoader.class)
    public void testValidationProvidersCanBeLoadedIfContextLoaderIsNull() {
        List<ValidationProvider<?>> validationProviders = providerResolver.getValidationProviders();

        assertEquals(2, validationProviders.size());
    }
}
