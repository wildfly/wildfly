/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation.testprovider;

import jakarta.validation.Configuration;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.spi.BootstrapState;
import jakarta.validation.spi.ConfigurationState;
import jakarta.validation.spi.ValidationProvider;

/**
 * A {@link ValidationProvider} implementation for testing purposes.
 *
 * @author Gunnar Morling
 */
public class MyValidationProvider implements ValidationProvider<MyConfiguration> {

    @Override
    public MyConfiguration createSpecializedConfiguration(BootstrapState state) {
        return new MyConfiguration();
    }

    @Override
    public Configuration<?> createGenericConfiguration(BootstrapState state) {
        return new MyConfiguration();
    }

    @Override
    public ValidatorFactory buildValidatorFactory(ConfigurationState configurationState) {
        return new MyValidatorFactoryImpl();
    }
}
