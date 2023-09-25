/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import jakarta.ejb.Local;
import jakarta.ejb.Stateful;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful
@Local(StatelessLocal.class)
public class StatelessBean implements StatelessLocal {
    public void hello() {
    }

    public void goodbye() {
    }
}
