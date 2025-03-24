/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation;

import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * This class lazily initialize the ValidatorFactory on the first usage One benefit is that no domain class is loaded until the
 * ValidatorFactory is really needed. Useful to avoid loading classes before Jakarta Persistence is initialized and has enhanced its classes.
 *
 * @author Emmanuel Bernard
 * @author Stuart Douglas
 */
public class LazyValidatorFactory implements ValidatorFactory {

    private final ClassLoader classLoader;

    private volatile ValidatorFactory delegate; // use as a barrier

    public LazyValidatorFactory(ClassLoader classLoader) {
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

    public void replaceDelegate(ValidatorFactory validatorFactory) {
        synchronized (this) {
            delegate = validatorFactory;
        }
    }

    @Override
    public Validator getValidator() {
        return getDelegate().getValidator();
    }

    private ValidatorFactory initFactory() {
        final ClassLoader oldTCCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return Validation.byDefaultProvider().providerResolver(new WildFlyProviderResolver()).configure()
                    .constraintValidatorFactory( new ConstraintValidatorFactory() {

                        @Override
                        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                            return getDelegate().getConstraintValidatorFactory().getInstance( key );
                        }

                        @Override
                        public void releaseInstance(ConstraintValidator<?, ?> instance) {
                            getDelegate().getConstraintValidatorFactory().releaseInstance( instance );
                        }
                    } )
                    .buildValidatorFactory();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTCCL);
        }
    }

    @Override
    public ValidatorContext usingContext() {
        final ClassLoader oldTCCL = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            // Make sure the deployment's CL is set
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            return getDelegate().usingContext();
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTCCL);
        }
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
        // Avoid initializing delegate if closing it
        if (delegate != null) {
            getDelegate().close();
        }
    }
}
