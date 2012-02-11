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
 * Does not allow invocation of HttpSessionListener or HttpSessionAttributeListener during session expiration due to undeploy.
 * @author Brian Stansberry
 */
public class IgnoreUndeployLegacyClusteredSessionNotificationPolicy extends LegacyClusteredSessionNotificationPolicy {
    /**
     * Overrides superclass to return <code>false</code> if the cause of the notification is {@link ClusteredSessionNotificationCause#UNDEPLOY}.
     * @return <code>true</code> if <code>status.isLocallyUsed()</code> is <code>true</code> and the cause of the notification
     *         is not {@link ClusteredSessionNotificationCause#UNDEPLOY}.
     */
    @Override
    public boolean isHttpSessionAttributeListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName, boolean local) {
        return !ClusteredSessionNotificationCause.UNDEPLOY.equals(cause) && super.isHttpSessionAttributeListenerInvocationAllowed(status, cause, attributeName, local);
    }

    /**
     * Overrides superclass to return <code>false</code> if the cause of the notification is {@link ClusteredSessionNotificationCause#UNDEPLOY}.
     * @return <code>true</code> if <code>status.isLocallyUsed()</code> is <code>true</code> and the cause of the notification
     *         is not {@link ClusteredSessionNotificationCause#UNDEPLOY}.
     */
    @Override
    public boolean isHttpSessionListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, boolean local) {
        return !ClusteredSessionNotificationCause.UNDEPLOY.equals(cause) && super.isHttpSessionListenerInvocationAllowed(status, cause, local);
    }
}
