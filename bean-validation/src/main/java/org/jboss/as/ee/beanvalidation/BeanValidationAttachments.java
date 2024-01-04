/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.beanvalidation;

import org.jboss.as.server.deployment.AttachmentKey;

import jakarta.validation.ValidatorFactory;

/**
 * @author Stuart Douglas
 */
public class BeanValidationAttachments {

    public static final AttachmentKey<ValidatorFactory> VALIDATOR_FACTORY = AttachmentKey.create(ValidatorFactory.class);

    private BeanValidationAttachments() {

    }
}
