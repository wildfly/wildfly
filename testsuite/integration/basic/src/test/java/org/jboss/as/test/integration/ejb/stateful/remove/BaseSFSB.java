/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.remove;

import jakarta.ejb.Remove;

/**
 * @author Jaikiran Pai
 */
public class BaseSFSB {

    @Remove(retainIfException = true)
    public void baseRetainIfAppException() {
        throw new SimpleAppException();
    }

    @Remove
    public void baseRemoveEvenIfAppException() {
        throw new SimpleAppException();
    }

    @Remove
    public void baseJustRemove() {
        // do nothing
    }
}
