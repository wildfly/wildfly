/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.beanclass.validity;

import jakarta.ejb.Stateful;

/**
 * Invalid use of @Stateful annotation on an interface
 *
 * User: Jaikiran Pai
 */
@Stateful
public interface StatefulOnAInterface {
}
