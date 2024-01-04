/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation.testprovider;

import java.io.InputStream;

import jakarta.validation.BootstrapConfiguration;
import jakarta.validation.ClockProvider;
import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.ParameterNameProvider;
import jakarta.validation.TraversableResolver;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.valueextraction.ValueExtractor;

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
