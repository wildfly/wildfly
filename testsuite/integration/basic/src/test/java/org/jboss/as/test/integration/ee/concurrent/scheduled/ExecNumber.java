/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.concurrent.scheduled;

public interface ExecNumber {

    void cease();

    void start();

    int actual();

    int expected();
}
