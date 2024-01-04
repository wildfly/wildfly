/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.util;

public abstract class TaintedBase {
    @SuppressWarnings("unused")
    public static void publicStatic() {
    }

    @SuppressWarnings("unused")
    public final void publicFinal() {
    }
}
