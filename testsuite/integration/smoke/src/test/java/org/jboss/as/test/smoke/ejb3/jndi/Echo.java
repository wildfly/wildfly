/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.jndi;

/**
 * @author Jaikiran Pai
 */
public interface Echo {

    String echo(String msg);
}
