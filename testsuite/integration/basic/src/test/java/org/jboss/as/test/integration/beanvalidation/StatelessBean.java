/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.beanvalidation;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * User: Jaikiran Pai
 */
@Stateless
public class StatelessBean {

    @Resource
    private Validator validator;

    @Resource
    private ValidatorFactory validatorFactory;

    public void buildDefaultValidatorFactory() {
        Validation.buildDefaultValidatorFactory();
    }

    public boolean isValidatorInjected() {
        return this.validator != null;
    }

    public boolean isValidatorFactoryInjected() {
        return this.validatorFactory != null;
    }

    public Validator getValidatorFromValidatorFactory() {
        return this.validatorFactory.getValidator();
    }
}
