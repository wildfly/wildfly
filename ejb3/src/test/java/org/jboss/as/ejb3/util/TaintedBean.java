/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.util;

public class TaintedBean extends TaintedBase {
    @SuppressWarnings("unused")
    public int cleanMethod1() {
        return 1;
    }
}
