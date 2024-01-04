/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.util;

import org.wildfly.security.manager.WildFlySecurityManager;

import jakarta.validation.ClockProvider;
import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;

/**
 * This class lazily initialize the ValidatorFactory on the first usage One benefit is that no domain class is loaded until the
 * ValidatorFactory is really needed. Useful to avoid loading classes before Jakarta Persistence is initialized and has enhanced its classes.
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
