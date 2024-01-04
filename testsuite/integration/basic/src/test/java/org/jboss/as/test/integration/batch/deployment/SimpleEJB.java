/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.batch.deployment;

import jakarta.ejb.Singleton;

/**
 * Pointless EJB that exists only to convert a jar to an EJB archive.
 */
@Singleton
public class SimpleEJB {

    public void noop() {
        // do nothing
    }
}
