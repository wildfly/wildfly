/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.beanclass.validity;

import jakarta.ejb.MessageDriven;

/**
 * Invalid use of {@link MessageDriven} annotation on an interface
 *
 * User: Jaikiran Pai
 */
@MessageDriven
public interface MessageDrivenOnAInterface {
}
