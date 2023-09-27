/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import jakarta.ejb.Remote;

/**
 * @author Ondrej Chaloupka
 */
@Remote
public interface AsyncBeanSynchronizeSingletonRemote {
    void reset();
    void latchCountDown();
    void latch2CountDown();
    void latchAwaitSeconds(int sec) throws InterruptedException;
    void latch2AwaitSeconds(int sec) throws InterruptedException;
}
