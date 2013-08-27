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
package org.jboss.as.controller.access;


/**
 * Encapsulates authorization information about an MBean call.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JmxAction {
    private final String methodName;
    private final Impact impact;

    public JmxAction(String methodName, Impact impact) {
        this.methodName = methodName;
        this.impact = impact;
    }

    /**
     * Gets the impact of the call
     *
     * @return the impact
     */
    public Impact getImpact() {
        return impact;
    }

    /**
     * Gets the {@link javax.management.MBeanServer} method name that was called
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * The impact of the call
     */
    public enum Impact {
        /** The call is read-only */
        READ_ONLY,
        /** The call writes data */
        WRITE,
        /** The call is special, and will normally only work for a (@link org.jboss.as.controller.access.rbac.StandardRole#SUPERUSER} or a (@link org.jboss.as.controller.access.rbac.StandardRole#ADMINISTRATOR} */
        EXTRA_SENSITIVE
    }
}
