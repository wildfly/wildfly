/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.authorization;

/**
 * @author Jaikiran Pai
 */
public interface TimeProvider {
    long getTime();
}
