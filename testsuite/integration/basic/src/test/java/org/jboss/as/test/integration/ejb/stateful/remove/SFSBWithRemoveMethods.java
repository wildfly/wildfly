/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.remove;

import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

/**
 * User: jpai
 */
@Stateful
public class SFSBWithRemoveMethods extends BaseSFSB {

    @Remove
    public void remove() {
        // do nothing
    }

    @Remove(retainIfException = true)
    public void retainIfAppException() {
        throw new SimpleAppException();
    }

    @Remove
    public void removeEvenIfAppException() {
        throw new SimpleAppException();
    }

    public void doNothing() {

    }
}
