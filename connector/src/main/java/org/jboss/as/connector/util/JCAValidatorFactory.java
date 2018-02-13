/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.util;

import org.wildfly.security.manager.WildFlySecurityManager;

import javax.validation.ClockProvider;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

/**
 * This class lazily initialize the ValidatorFactory on the first usage One benefit is that no domain class is loaded until the
 * ValidatorFactory is really needed. Useful to avoid loading classes before JPA is initialized and has enhanced its classes.
 * <p/>
 * Note: This class is a copy of {@code org.jboss.as.ee.beanvalidation.LazyValidatorFactory}.
 *
 * @author Emmanuel Bernard
 * @author Stuart Douglas
 */
public class JCAValidatorFactory implements ValidatorFactory {

    private final Configuration<?> configuration;
    private final ClassLoader classLoader;

    private volatile ValidatorFactory delegate; // use as a barrier

    /**
     * Use the default ValidatorFactory creation routine
     */
    public JCAValidatorFactory(ClassLoader classLoader) {
        this(null, classLoader);
    }

    public JCAValidatorFactory(Configuration<?> configuration, ClassLoader classLoader) {
        this.configuration = configuration;
        this.classLoader = classLoader;
    }

    private ValidatorFactory getDelegate() {
        ValidatorFactory result = delegate;
        if (result == null) {
            synchronized (this) {
                result = delegate;
                if (result == null) {
                    delegate = result = initFactory();
                }
            }
        }
        return result;
    }

    @Override
    public Validator getValidator() {
        return getDelegate().getValidator();
    }

    private ValidatorFactory initFactory() {
        final ClassLoader oldTCCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            if (configuration == null) {
                return Validation.byDefaultProvider().providerResolver(new WildFlyProviderResolver()).configure()
                        .buildValidatorFactory();

            } else {
                return configuration.buildValidatorFactory();
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTCCL);
        }
    }

    @Override
    public ValidatorContext usingContext() {
        return getDelegate().usingContext();
    }

    @Override
    public MessageInterpolator getMessageInterpolator() {
        return getDelegate().getMessageInterpolator();
    }

    @Override
    public TraversableResolver getTraversableResolver() {
        return getDelegate().getTraversableResolver();
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return getDelegate().getConstraintValidatorFactory();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        return getDelegate().getParameterNameProvider();
    }

    @Override
    public ClockProvider getClockProvider() {
        return getDelegate().getClockProvider();
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return getDelegate().unwrap(clazz);
    }

    @Override
    public void close() {
        getDelegate().close();
    }
}
