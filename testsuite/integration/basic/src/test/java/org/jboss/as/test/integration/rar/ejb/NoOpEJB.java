/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.rar.ejb;

import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
public class NoOpEJB {

    public void doNothing() {

    }
}
