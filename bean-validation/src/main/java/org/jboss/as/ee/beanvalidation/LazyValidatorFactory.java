/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.validation.ClockProvider;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * This class lazily initialize the ValidatorFactory on the first usage One benefit is that no domain class is loaded until the
 * ValidatorFactory is really needed. Useful to avoid loading classes before JPA is initialized and has enhanced its classes.
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
