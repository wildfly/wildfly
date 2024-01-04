/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.structure.parsing;

import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
public class NoOpEJB {

    public void doNothing() {
        return;
    }
}
