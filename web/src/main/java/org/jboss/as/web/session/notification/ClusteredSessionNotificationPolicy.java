/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.notification;

/**
 * Policy for determining whether the servlet spec notifications related to session events are allowed to be emitted on the
 * local cluster node.
 * <p>
 * <strong>Note:</strong> The use of the word <strong>allowed</strong> above is intentional; if a given policy implementation
 * returns <code>true</code> from one of the methods in this interface, that does not mean the listener will be invoked by the
 * container, nor does the presence of a method in this interface imply that it will be invoked by the container in all cases.
 * The only contract this interface creates is that before invoking a listener method, the container will invoke an
 * implementation of this policy to get permission and will not invoke the listeners if this policy returns <code>false</code>.
 * If the container does not support emitting notifications in certain cases, it may not bother checking if the notification is
 * allowed, and even if it checks, it still will not emit the notification.
 * </p>
 * <p>
 * An example of a case where the container may not support emitting a notification is for a session that has never been used
 * locally.
 * </p>
 * @author Brian Stansberry
 */
public interface ClusteredSessionNotificationPolicy {
    /**
     * Are invocations of <code>HttpSessionListener</code> callbacks allowed under the given conditions?
     * @param status the status of the session
     * @param cause the cause of the session notification
     * @param local <code>true</code> if the event driving the notification originated on this node; <code>false</code> otherwise
     * @return <code>true</code> if the notification is allowed, <code>false</code> if not
     */
    boolean isHttpSessionListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, boolean local);

    /**
     * Under the given conditions, are invocations of <code>HttpSessionAttributeListener</code> callbacks allowed?
     * @param status the status of the session
     * @param cause the cause of the session notification
     * @param attributeName value that would be passed to the <code>name</code> param of the
     *        <code>HttpSessionBindingEvent</code> if the listener were invoked
     * @param local <code>true</code> if the event driving the notification originated on this node; <code>false</code> otherwise
     * @return <code>true</code> if the notification is allowed, <code>false</code> if not
     */
    boolean isHttpSessionAttributeListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName, boolean local);

    /**
     * Under the given conditions, are invocations of <code>HttpSessionBindingListener</code> callbacks allowed?
     * @param status the status of the session
     * @param cause the cause of the session notification
     * @param attributeName value that would be passed to the <code>name</code> param of the
     *        <code>HttpSessionBindingEvent</code> if the listener were invoked
     * @param local <code>true</code> if the event driving the notification originated on this node; <code>false</code> otherwise
     * @return <code>true</code> if the notification is allowed, <code>false</code> if not
     */
    boolean isHttpSessionBindingListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName, boolean local);

    /**
     * Under the given conditions, are invocations of <code>HttpSessionActivationListener</code> callbacks allowed?
     * @param status the status of the session
     * @param cause the cause of the session notification
     * @param attributeName value that would be passed to the <code>name</code> param of the <code>HttpSessionEvent</code> if
     *        the listener were invoked
     * @return <code>true</code> if the notification is allowed, <code>false</code> if not
     */
    boolean isHttpSessionActivationListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName);

    /**
     * Provides the policy information about the container's capabilities with respect to issuing notifications. Will be invoked
     * by the container before the first invocation of any of the other methods in this interface.
     * @param capability the capability, Will not be <code>null</code>.
     */
    void setClusteredSessionNotificationCapability(ClusteredSessionNotificationCapability capability);
}
