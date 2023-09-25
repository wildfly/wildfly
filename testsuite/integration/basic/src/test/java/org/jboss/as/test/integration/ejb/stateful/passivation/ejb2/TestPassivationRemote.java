/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

/**
 * @author Ondrej Chaloupka
 */
public interface TestPassivationRemote extends jakarta.ejb.EJBObject {
    String EXPECTED_RESULT = "true";

    /**
     * Returns the expected result exposed as a static final variable by this interface
     */
    String returnTrueString();

    /**
     * Returns whether or not this instance has been passivated
     */
    boolean hasBeenPassivated();

    /**
     * Returns whether or not this instance has been activated
     */
    boolean hasBeenActivated();
}
