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
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/**
 * @author Jaikiran Pai
 */
@Singleton
@LocalBean
@TransactionManagement(value = TransactionManagementType.BEAN)
public class LongWritesSingletonBean {

    private int count;

    @AccessTimeout(value = 1, unit = TimeUnit.SECONDS)
    public void fiveSecondWriteOperation() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        count++;
    }

    @Lock(value = LockType.READ)
    public int getCount() {
        return this.count;
    }
}
