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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorFactory;
import org.jboss.as.ee.beanvalidation.testprovider.MyValidatorImpl;
import org.jboss.as.ee.beanvalidation.testutil.WithContextClassLoader;
import org.jboss.as.ee.beanvalidation.testutil.ContextClassLoaderRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Unit test for {@link LazyValidatorFactory}.
 *
 * @author Gunnar Morling
 */
public class LazyValidatorFactoryTestCase {

    @Rule
    public final ContextClassLoaderRule contextClassLoaderRule = new ContextClassLoaderRule();

    private ValidatorFactory validatorFactory;

    @Before
    public void setupValidatorFactory() {
        validatorFactory = new LazyValidatorFactory(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Test
    public void testHibernateValidatorIsUsedAsProviderByDefault() {
        HibernateValidatorFactory hibernateValidatorFactory = validatorFactory.unwrap(HibernateValidatorFactory.class);
        assertNotNull("LazyValidatorFactory should delegate to the HV factory by default", hibernateValidatorFactory);

        Validator validator = validatorFactory.getValidator();
        assertNotNull("LazyValidatorFactory should provide a validator", validator);
    }

    @Test
    @WithContextClassLoader(TestClassLoader.class)
    public void testSpecificProviderCanBeConfiguredInValidationXml() {
        Validator validator = validatorFactory.getValidator();
        assertNotNull("LazyValidatorFactory should provide a validator", validator);
        assertTrue("Validator should be of type created by XML-configured provider", validator instanceof MyValidatorImpl);
    }

    /**
     * A class loader which makes the file {@code custom-default-validation-provider-validation.xml} available as
     * {@code META-INF/validation.xml}.
     *
     * @author Gunnar Morling
     */
    public static final class TestClassLoader extends ClassLoader {

        public TestClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            if (name.equals("META-INF/validation.xml")) {
                return LazyValidatorFactoryTestCase.class.getResourceAsStream("custom-default-validation-provider-validation.xml");
            }

            return super.getResourceAsStream(name);
        }
    }
}
