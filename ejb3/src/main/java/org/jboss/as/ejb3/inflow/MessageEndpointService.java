/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.inflow;

import javax.transaction.TransactionManager;
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
