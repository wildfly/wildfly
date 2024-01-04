/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.concurrency;

import java.util.concurrent.TimeUnit;
import jakarta.ejb.AccessTimeout;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;

/**
 * @author Jaikiran Pai
 */
@Singleton
@LocalBean
@Lock(value = LockType.READ)
public class ReadOnlySingletonBean implements ReadOnlySingleton {


    @AccessTimeout(value = 1, unit = TimeUnit.SECONDS)
    public String twoSecondEcho(String msg) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return msg;
    }
}
