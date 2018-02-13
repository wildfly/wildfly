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
package org.jboss.as.ee.beanvalidation.testprovider;

import java.io.InputStream;

import javax.validation.BootstrapConfiguration;
import javax.validation.ClockProvider;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.ValidatorFactory;
import javax.validation.valueextraction.ValueExtractor;

/**
 * A {@link Configuration} implementation for testing purposes.
 *
 * @author Gunnar Morling
 */
public class MyConfiguration implements Configuration<MyConfiguration> {

    @Override
    public MyConfiguration ignoreXmlConfiguration() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration messageInterpolator(MessageInterpolator interpolator) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration traversableResolver(TraversableResolver resolver) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration constraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration parameterNameProvider(ParameterNameProvider parameterNameProvider) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration addMapping(InputStream stream) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration addProperty(String name, String value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MessageInterpolator getDefaultMessageInterpolator() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public TraversableResolver getDefaultTraversableResolver() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ConstraintValidatorFactory getDefaultConstraintValidatorFactory() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ParameterNameProvider getDefaultParameterNameProvider() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public BootstrapConfiguration getBootstrapConfiguration() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration clockProvider(ClockProvider clockProvider) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public MyConfiguration addValueExtractor(ValueExtractor<?> extractor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ClockProvider getDefaultClockProvider() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ValidatorFactory buildValidatorFactory() {
        return new MyValidatorFactoryImpl();
    }
}
