/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.bean;

public interface Incrementor {
    Result<Integer> increment();

    void remove();
}
