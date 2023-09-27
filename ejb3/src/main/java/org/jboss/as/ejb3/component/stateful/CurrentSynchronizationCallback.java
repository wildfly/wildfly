/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

/**
 * Thread local that tracks the current state of SFSB synchronization callbacks
 *
 * @author Stuart Douglas
 */
public class CurrentSynchronizationCallback {

    public enum CallbackType {
        AFTER_BEGIN,
        AFTER_COMPLETION,
        BEFORE_COMPLETION
    }

    private static final ThreadLocal<CallbackType> type = new ThreadLocal<CallbackType>();

    public static CallbackType get() {
        return type.get();
    }

    public static void set(CallbackType callback) {
        type.set(callback);
    }

    public static void clear() {
        //set to null instead of removing to prevent thread local entry churn
        type.set(null);
    }

}
