/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.beanclass.validity;

import jakarta.ejb.Stateless;

/**
 * Invalid use of @Stateless annotation on an interface
 *
 * User: Jaikiran Pai
 */
@Stateless
public interface StatelessOnAInterface {
}
