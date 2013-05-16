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
package org.jboss.as.jpa.validator;

import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

/**
 * This class lazily initialize the ValidatorFactory on the first usage
 * One benefit is that no domain class is loaded until the
 * ValidatorFactory is really needed.
 * Useful to avoid loading classes before JPA is initialized
 * and has enhanced its classes.
 *
 * @author Emmanuel Bernard
 * @author Stuart Douglas
 */
public class JPALazyValidatorFactory implements ValidatorFactory {


    private volatile ValidatorFactory delegate; //use as a barrier

    public JPALazyValidatorFactory() {
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

    public Validator getValidator() {
        return getDelegate().getValidator();
    }

    private ValidatorFactory initFactory() {
        final ClassLoader oldTCCL = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(oldTCCL);
            ConstraintMapping mapping = new ConstraintMapping();
            HibernateValidatorConfiguration config = Validation.byProvider(HibernateValidator.class).providerResolver(new JbossProviderResolver()).configure();
            config.addMapping(mapping);
            ValidatorFactory factory = config.buildValidatorFactory();
            return factory;
        } finally {
            SecurityActions.setContextClassLoader(oldTCCL);
        }
    }

    public ValidatorContext usingContext() {
        return getDelegate().usingContext();
    }

    public MessageInterpolator getMessageInterpolator() {
        return getDelegate().getMessageInterpolator();
    }

    public TraversableResolver getTraversableResolver() {
        return getDelegate().getTraversableResolver();
    }

    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        return getDelegate().getConstraintValidatorFactory();
    }

    public <T> T unwrap(Class<T> clazz) {
        return getDelegate().unwrap(clazz);
    }

    private static final class JbossProviderResolver implements ValidationProviderResolver {

        @Override
        public List<ValidationProvider<?>> getValidationProviders() {
            return Collections.<ValidationProvider<?>>singletonList(new HibernateValidator());
        }
    }
}
