/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.singleton.concurrency.inheritance;

import java.util.concurrent.CountDownLatch;
import jakarta.ejb.AccessTimeout;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;

/**
 * @author Stuart Douglas
 */
@Singleton
@Lock(LockType.READ)
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class SingletonChildBean extends SingletonBaseBean {


    //this is now a read lock
    @AccessTimeout(value = 0)
    public void writeLockOverriddenByParent(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        super.writeLockOverriddenByParent(cont, entered);
    }

    @Lock(LockType.WRITE)
    @AccessTimeout(value = 0)
    public void readLockOverriddenByParent(CountDownLatch cont, CountDownLatch entered) throws InterruptedException {
        super.readLockOverriddenByParent(cont, entered);
    }

}
