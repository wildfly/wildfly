/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.concurrency;

/**
 * @author Ondrej Chaloupka
 */
public interface ReadOnlySingleton {
    String twoSecondEcho(String msg);
}
