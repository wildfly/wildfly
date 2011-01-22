/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.beanvalidation;

import org.apache.commons.collections.iterators.ArrayListIterator;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

import javax.validation.Validation;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.spi.ValidationProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Return the appropriate ValidatorFactory
 * TODO:  either cache the ValidatorFactory or add dynamic support for more validators (or per app validators)
 *
 *
 *
 * @author Scott Marlow (based on code from Emmanuel Bernard)
 */
public class ValidatorFactoryProvider implements ValidationProviderResolver {

    private static final ValidatorFactoryProvider INSTANCE = new ValidatorFactoryProvider();

    private static final List<ValidationProvider<?>> validationProvider = new ArrayList<ValidationProvider<?>>(1);
    static {
        validationProvider.add(new HibernateValidator());
    }

    public static ValidatorFactoryProvider getInstance() {
        return INSTANCE;
    }

    public Validator getValidator() {
        return getValidatorFactory().getValidator();
    }

    public ValidatorFactory getValidatorFactory() {
        ConstraintMapping mapping = new ConstraintMapping();
        HibernateValidatorConfiguration config = Validation.byProvider( HibernateValidator.class ).providerResolver(this).configure();
        config.addMapping( mapping );
        ValidatorFactory factory = config.buildValidatorFactory();
        return factory;
    }


    @Override
    public List<ValidationProvider<?>> getValidationProviders() {
        return validationProvider;
    }
}
