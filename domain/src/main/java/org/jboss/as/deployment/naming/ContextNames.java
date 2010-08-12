/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.naming;

import org.jboss.msc.service.ServiceName;

/**
 * Common names used for deploying naming related services at different scopes.   
 *
 * @author John E. Bailey
 */
public class ContextNames {
    /**
     * Parent ServiceName for all naming services.
     */
    public static final ServiceName NAMING = ServiceName.JBOSS.append("naming");

    /**
     * ServiceName for java: namespace
     */
    public static final ServiceName JAVA = NAMING.append("java");

    /**
     * ServiceName for java:global namespace
     */
    public static final ServiceName GLOBAL = JAVA.append("global");

    /**
     * Parent ServiceName for java:app namespaces
     */
    public static final ServiceName APPLICATION = JAVA.append("application");

    /**
     * Parent ServiceName for java:module namespaces
     */
    public static final ServiceName MODULE = JAVA.append("module");

    /**
     * Parent ServiceName for java:comp namespaces
     */
    public static final ServiceName COMPONENT = JAVA.append("component");
}
