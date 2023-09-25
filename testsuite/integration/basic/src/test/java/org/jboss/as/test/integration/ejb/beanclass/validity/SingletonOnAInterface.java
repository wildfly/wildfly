/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.beanclass.validity;

import jakarta.ejb.Singleton;

/**
 * Invalid use of @Singleton annotation on an interface
 *
 * User: Jaikiran Pai
 */
@Singleton
public interface SingletonOnAInterface {
}
