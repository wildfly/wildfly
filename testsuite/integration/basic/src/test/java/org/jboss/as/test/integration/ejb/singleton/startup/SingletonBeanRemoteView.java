/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup;

/**
 * User: jpai
 */
public interface SingletonBeanRemoteView {

    void doSomething();

    String echo(String msg);
}
