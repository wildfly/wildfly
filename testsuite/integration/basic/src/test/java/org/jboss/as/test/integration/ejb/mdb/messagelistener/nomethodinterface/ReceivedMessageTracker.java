/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagelistener.nomethodinterface;

import java.util.concurrent.CountDownLatch;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;

/**
 * @author Jan Martiska
 */
@Singleton
public class ReceivedMessageTracker {

    private CountDownLatch received;

    @PostConstruct
    public void init() {
        received = new CountDownLatch(1);
    }

    public CountDownLatch getReceivedLatch() {
        return received;
    }

}
