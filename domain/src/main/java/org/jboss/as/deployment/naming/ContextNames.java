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
     * Jndi name for java: namespace
     */
    public static final JndiName JAVA_CONTEXT_NAME = JndiName.of("java:");

    /**
     * ServiceName for java: namespace
     */
    public static final ServiceName JAVA_CONTEXT_SERVICE_NAME = NAMING.append("context", "java");

    /**
     * Jndi name for java:global namespace
     */
    public static final JndiName GLOBAL_CONTEXT_NAME = JndiName.of("java:global");

    /**
     * ServiceName for java:global namespace
     */
    public static final ServiceName GLOBAL_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("global");

    /**
     * Jndi name for java:app namespace
     */
    public static final JndiName APPLICATION_CONTEXT_NAME = JndiName.of("java:app");

    /**
     * Parent ServiceName for java:app namespace
     */
    public static final ServiceName APPLICATION_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("application");

    /**
     * Jndi name for java:module namespace
     */
    public static final JndiName MODULE_CONTEXT_NAME = JndiName.of("java:module");

    /**
     * Parent ServiceName for java:module namespace
     */
    public static final ServiceName MODULE_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("module");

    /**
     * Jndi name for java:comp namespace
     */
    public static final JndiName COMPONENT_CONTEXT_NAME = JndiName.of("java:comp");


    /**
     * Parent ServiceName for java:comp namespace
     */
    public static final ServiceName COMPONENT_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("component");
}
