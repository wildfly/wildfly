/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.locking.reentrant;

import jakarta.ejb.AccessTimeout;
import jakarta.ejb.Stateful;
import java.util.concurrent.TimeUnit;

/**
 * stateful session bean
 *
 */
@Stateful
@AccessTimeout(value=2, unit = TimeUnit.SECONDS)
public class SimpleSFSB {

    public void doStuff() {
    }

}
