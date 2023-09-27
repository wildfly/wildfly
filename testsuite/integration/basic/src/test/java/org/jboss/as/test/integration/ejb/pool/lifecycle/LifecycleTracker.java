/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.pool.lifecycle;

/**
 * @author baranowb
 *
 */
public interface LifecycleTracker {

    void trackPostConstructOn(final String beanImplementationClassName);
    void trackPreDestroyOn(final String beanImplementationClassName);
    void clearState();
    boolean wasPostConstructInvokedOn(final String beanImplClassName);
    boolean wasPreDestroyInvokedOn(final String beanImplClassName);
}