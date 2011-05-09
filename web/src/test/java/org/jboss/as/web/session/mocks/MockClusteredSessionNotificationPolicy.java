/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.mocks;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.web.session.notification.ClusteredSessionManagementStatus;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationCause;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationPolicy;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationPolicyBase;

/**
 * @author Brian Stansberry
 * 
 */
public class MockClusteredSessionNotificationPolicy extends ClusteredSessionNotificationPolicyBase implements
        ClusteredSessionNotificationPolicy {
    private boolean response;
    public final List<PolicyInvocation> invocations = new ArrayList<PolicyInvocation>();

    public enum Type {
        ACTIVATION, ATTRIBUTE, BINDING, SESSION
    };

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.web.tomcat.service.session.notification.ClusteredSessionNotificationPolicy#
     * isHttpSessionAttributeListenerInvocationAllowed
     * (org.jboss.web.tomcat.service.session.notification.ClusteredSessionManagementStatus,
     * org.jboss.web.tomcat.service.session.notification.ClusteredSessionNotificationCause, java.lang.String, boolean)
     */
    public boolean isHttpSessionAttributeListenerInvocationAllowed(ClusteredSessionManagementStatus status,
            ClusteredSessionNotificationCause cause, String attributeName, boolean local) {
        invocations.add(new PolicyInvocation(Type.ATTRIBUTE, status, cause, attributeName, local));
        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.web.tomcat.service.session.notification.ClusteredSessionNotificationPolicy#
     * isHttpSessionBindingListenerInvocationAllowed
     * (org.jboss.web.tomcat.service.session.notification.ClusteredSessionManagementStatus,
     * org.jboss.web.tomcat.service.session.notification.ClusteredSessionNotificationCause, java.lang.String, boolean)
     */
    public boolean isHttpSessionBindingListenerInvocationAllowed(ClusteredSessionManagementStatus status,
            ClusteredSessionNotificationCause cause, String attributeName, boolean local) {
        invocations.add(new PolicyInvocation(Type.BINDING, status, cause, attributeName, local));
        return response;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.web.tomcat.service.session.notification.ClusteredSessionNotificationPolicy#isHttpSessionListenerInvocationAllowed
     * (org.jboss.web.tomcat.service.session.notification.ClusteredSessionManagementStatus,
     * org.jboss.web.tomcat.service.session.notification.ClusteredSessionNotificationCause, boolean)
     */
    public boolean isHttpSessionListenerInvocationAllowed(ClusteredSessionManagementStatus status,
            ClusteredSessionNotificationCause cause, boolean local) {
        invocations.add(new PolicyInvocation(Type.SESSION, status, cause, null, local));
        return response;
    }

    public boolean isHttpSessionActivationListenerInvocationAllowed(ClusteredSessionManagementStatus status,
            ClusteredSessionNotificationCause cause, String attributeName) {
        invocations.add(new PolicyInvocation(Type.ACTIVATION, status, cause, attributeName, true));
        return response;
    }

    public boolean getResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }

    public List<PolicyInvocation> getInvocations() {
        return invocations;
    }

    public void clear() {
        invocations.clear();
    }

    public static class PolicyInvocation {
        public final Type type;
        public final ClusteredSessionManagementStatus status;
        public final ClusteredSessionNotificationCause cause;
        public final String attributeName;
        public final boolean local;

        private PolicyInvocation(Type type, ClusteredSessionManagementStatus status, ClusteredSessionNotificationCause cause,
                String attributeName, boolean local) {
            this.type = type;
            this.status = status;
            this.cause = cause;
            this.attributeName = attributeName;
            this.local = local;
        }
    }

}
