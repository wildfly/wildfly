/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.util;

public class CleanBean implements BusinessInterface {
    @SuppressWarnings("unused")
    @Override
    public void businessMethod(String argumentOne, int argumentTwo) {
    }

    @SuppressWarnings("unused")
    @Override
    public void businessMethod(String argumentOne, int argumentTwo, boolean argumentThree) {
    }

    @SuppressWarnings("unused")
    public void notABusinessMethod(String argumentOne) {
    }

    @SuppressWarnings("unused")
    protected static void protectedStatic() {
    }

    @SuppressWarnings("unused")
    protected final void protectedFinal() {
    }

    @SuppressWarnings("unused")
    static void packageStatic() {
    }

    @SuppressWarnings("unused")
    final void packageFinal() {
    }
}
