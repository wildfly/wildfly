/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ee.beanvalidation;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

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
