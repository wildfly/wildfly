/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.containerstart;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface HelperSingleton {
    int await(String where, long timeout, TimeUnit unit) throws BrokenBarrierException, TimeoutException, InterruptedException;
    void reset();
}
