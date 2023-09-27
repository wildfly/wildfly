/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules;

public class DeltaBean implements Comparable<Integer> {
    @Override
    public int compareTo(Integer o) {
        return -1;
    }
}
