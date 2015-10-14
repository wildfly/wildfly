/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.allowedmethods;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.interceptors.InvocationType;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.InterceptorContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class and its subclasses can be used to determine if a given method
 * is allowed to be invoked.
 *
 * @see CurrentInvocationContext
 * @see CurrentSynchronizationCallback
 *
 * @author Stuart Douglas
 */
public class AllowedMethodsInformation {

    public static final AllowedMethodsInformation INSTANCE_BMT = new AllowedMethodsInformation(true);
    public static final AllowedMethodsInformation INSTANCE_CMT = new AllowedMethodsInformation(false);

    private final Set<DeniedMethodKey> denied;
    private final Set<DeniedSyncMethodKey> deniedSyncMethods;
    private final boolean beanManagedTransaction;

    protected AllowedMethodsInformation(boolean beanManagedTransaction) {
        this.beanManagedTransaction = beanManagedTransaction;
        final Set<DeniedMethodKey> denied = new HashSet<DeniedMethodKey>();
        add(denied, InvocationType.SET_ENTITY_CONTEXT, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.SET_ENTITY_CONTEXT, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.SET_ENTITY_CONTEXT, MethodType.GET_PRIMARY_KEY);
        add(denied, InvocationType.SET_ENTITY_CONTEXT, MethodType.GET_TIMER_SERVICE);
        add(denied, InvocationType.SET_ENTITY_CONTEXT, MethodType.IS_CALLER_IN_ROLE);
        add(denied, InvocationType.SET_ENTITY_CONTEXT, MethodType.GET_CALLER_PRINCIPLE);

        add(denied, InvocationType.HOME_METHOD, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.HOME_METHOD, MethodType.GET_PRIMARY_KEY);

        add(denied, InvocationType.ENTITY_EJB_CREATE, MethodType.TIMER_SERVICE_METHOD);
        add(denied, InvocationType.ENTITY_EJB_CREATE, MethodType.GET_PRIMARY_KEY);

        setup(denied);
        this.denied = Collections.unmodifiableSet(denied);

        final Set<DeniedSyncMethodKey> deniedSync = new HashSet<DeniedSyncMethodKey>();
        add(deniedSync, CurrentSynchronizationCallback.CallbackType.AFTER_COMPLETION, MethodType.TIMER_SERVICE_METHOD);
        add(deniedSync, CurrentSynchronizationCallback.CallbackType.AFTER_COMPLETION, MethodType.GET_ROLLBACK_ONLY);
        add(deniedSync, CurrentSynchronizationCallback.CallbackType.AFTER_COMPLETION, MethodType.SET_ROLLBACK_ONLY);


        this.deniedSyncMethods = Collections.unmodifiableSet(deniedSync);

    }

    protected void setup(Set<DeniedMethodKey> denied) {

    }

    protected static void add(Set<DeniedMethodKey> otherDenied, InvocationType setEntityContext, MethodType timerServiceMethod) {
        otherDenied.add(new DeniedMethodKey(setEntityContext, timerServiceMethod));
    }

    protected static void add(Set<DeniedSyncMethodKey> otherDenied, CurrentSynchronizationCallback.CallbackType callbackType, MethodType timerServiceMethod) {
        otherDenied.add(new DeniedSyncMethodKey(callbackType, timerServiceMethod));
    }

    /**
     * Checks that the current method
     */
    public static void checkAllowed(final MethodType methodType) {

        final InterceptorContext context = CurrentInvocationContext.get();
        if (context == null) {
            return;
        }

        final Component component = context.getPrivateData(Component.class);
        if (!(component instanceof EJBComponent)) {
            return;
        }
        final InvocationType invocationType = context.getPrivateData(InvocationType.class);

        ((EJBComponent) component).getAllowedMethodsInformation().realCheckPermission(methodType, invocationType);

    }

    /**
     * transaction sync is not affected by the current invocation, as multiple ejb methods may be invoked from afterCompletion
     */
    private void checkTransactionSync(MethodType methodType) {
        //first we have to check the synchronization status
        //as the sync is not affected by the current invocation
        final CurrentSynchronizationCallback.CallbackType currentSync = CurrentSynchronizationCallback.get();
        if (currentSync != null) {
            if (deniedSyncMethods.contains(new DeniedSyncMethodKey(currentSync, methodType))) {
                throwException(methodType, currentSync);
            }
        }
    }

    protected void realCheckPermission(MethodType methodType, InvocationType invocationType) {
        checkTransactionSync(methodType);
        if (invocationType != null) {
            if (denied.contains(new DeniedMethodKey(invocationType, methodType))) {
                throwException(methodType, invocationType);
            }
        }
        if (invocationType != InvocationType.CONCURRENT_CONTEXT && !beanManagedTransaction && methodType == MethodType.GET_USER_TRANSACTION) {
            throw EjbLogger.ROOT_LOGGER.unauthorizedAccessToUserTransaction();
        }
    }

    /**
     * throw an exception when a method cannot be invoked
     *
     * @param methodType     the method
     * @param invocationType the type of invocation that caused it to be disabled
     */
    protected void throwException(MethodType methodType, InvocationType invocationType) {
        throw EjbLogger.ROOT_LOGGER.cannotCallMethod(methodType.getLabel(), invocationType.getLabel());
    }


    /**
     * throw an exception when a method cannot be invoked
     *
     * @param methodType the method
     * @param callback   the type of invocation that caused it to be disabled
     */
    protected void throwException(MethodType methodType, CurrentSynchronizationCallback.CallbackType callback) {
        throw EjbLogger.ROOT_LOGGER.cannotCallMethod(methodType.getLabel(), callback.name());
    }


    private static class DeniedSyncMethodKey {
        private final CurrentSynchronizationCallback.CallbackType callbackType;
        private final MethodType methodType;

        public DeniedSyncMethodKey(CurrentSynchronizationCallback.CallbackType callbackType, MethodType methodType) {
            this.callbackType = callbackType;
            this.methodType = methodType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DeniedSyncMethodKey that = (DeniedSyncMethodKey) o;

            if (callbackType != that.callbackType) return false;
            if (methodType != that.methodType) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = callbackType != null ? callbackType.hashCode() : 0;
            result = 31 * result + (methodType != null ? methodType.hashCode() : 0);
            return result;
        }
    }
}
