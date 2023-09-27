/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.concurrency;

/**
 * @author Ondrej Chaloupka
 */
public class ReadOnlySingletonBeanDescriptorWithExpression implements ReadOnlySingleton {

    public String twoSecondEcho(String msg) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return msg;
    }
}
