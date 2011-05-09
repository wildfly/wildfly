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
 * {@link ClusteredSessionNotificationPolicy} implementation that describes the behavior of JBoss AS releases prior to 4.2.4.
 * @author Brian Stansberry
 */
public class LegacyClusteredSessionNotificationPolicy extends ClusteredSessionNotificationPolicyBase {
    /**
     * {@inheritDoc}
     * @return <code>true</code> if <code>status.isLocallyUsed()</code> is <code>true</code>.
     */
    @Override
    public boolean isHttpSessionAttributeListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName, boolean local) {
        return status.isLocallyUsed();
    }

    /**
     * {@inheritDoc}
     * @return <code>true</code> if <code>status.isLocallyUsed()</code> is <code>true</code>.
     */
    @Override
    public boolean isHttpSessionBindingListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName, boolean local) {
        return status.isLocallyUsed();
    }

    /**
     * {@inheritDoc}
     * @return <code>true</code> if <code>status.isLocallyUsed()</code> is <code>true</code>.
     */
    @Override
    public boolean isHttpSessionListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, boolean local) {
        return status.isLocallyUsed() && !ClusteredSessionNotificationCause.FAILOVER.equals(cause);
    }

    /**
     * {@inheritDoc}
     * @return <code>true</code> if <code>status.isLocallyUsed()</code> is <code>true</code>.
     */
    @Override
    public boolean isHttpSessionActivationListenerInvocationAllowed(ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause, String attributeName) {
        return status.isLocallyUsed();
    }
}
