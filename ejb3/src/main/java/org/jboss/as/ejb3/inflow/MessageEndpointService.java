/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.inflow;

import jakarta.transaction.TransactionManager;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface MessageEndpointService<T> {
    Class<T> getMessageListenerInterface();

    TransactionManager getTransactionManager();

    boolean isDeliveryTransacted(Method method) throws NoSuchMethodException;

    T obtain(long timeout, TimeUnit milliseconds);

    void release(T obj);

    /**
     * Returns the classloader that's applicable for the endpoint application.
     * This classloader will be used to set the thread context classloader during the beforeDelivery()/afterDelivery()
     * callbacks as mandated by the JCA 1.6 spec
     *
     * @return
     */
    ClassLoader getClassLoader();

    String getActivationName();
}
