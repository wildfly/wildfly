/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.serverside.remote;

public interface SimpleCounter {

    String getSimpleName();

    int getCount();

    void increment();

    void decrement();
}
