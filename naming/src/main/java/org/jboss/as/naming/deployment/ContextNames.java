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

package org.jboss.as.naming.deployment;

import org.jboss.msc.service.ServiceName;

/**
 * @author John Bailey
 */
public class ContextNames {

    /**
     * Parent ServiceName for all naming services.
     */
    public static final ServiceName NAMING = ServiceName.JBOSS.append("naming");
    /**
     * ServiceName for java: namespace
     */
    public static final ServiceName JAVA_CONTEXT_SERVICE_NAME = NAMING.append("context", "java");
    /**
     * Parent ServiceName for java:comp namespace
     */
    public static final ServiceName COMPONENT_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("comp");
    /**
     * Jndi name for java: namespace
     */
    private static final JndiName JAVA_CONTEXT_NAME = JndiName.of("java:");

    /**
     * Jndi name for java:jboss namespace
     */
    public static final JndiName JBOSS_CONTEXT_NAME = JndiName.of("java:jboss");

    /**
     * ServiceName for java:jboss namespace
     */
    public static final ServiceName JBOSS_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("jboss");

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
    public static final ServiceName APPLICATION_CONTEXT_SERVICE_NAME = JAVA_CONTEXT_SERVICE_NAME.append("app");

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
     * Get the base service name of a component's JNDI namespace.
     *
     * @param app    the application name (must not be {@code null})
     * @param module the module name (must not be {@code null})
     * @param comp   the component name (must not be {@code null})
     * @return the base service name
     */
    public static ServiceName contextServiceNameOfComponent(String app, String module, String comp) {
        return COMPONENT_CONTEXT_SERVICE_NAME.append(app, module, comp);
    }

    /**
     * Get the base service name of a module's JNDI namespace.
     *
     * @param app    the application name (must not be {@code null})
     * @param module the module name (must not be {@code null})
     * @return the base service name
     */
    public static ServiceName contextServiceNameOfModule(String app, String module) {
        return MODULE_CONTEXT_SERVICE_NAME.append(app, module);
    }

    /**
     * Get the base service name of an application's JNDI namespace.
     *
     * @param app the application name (must not be {@code null})
     * @return the base service name
     */
    public static ServiceName contextServiceNameOfApplication(String app) {
        return APPLICATION_CONTEXT_SERVICE_NAME.append(app);
    }

    /**
     * Get the service name of a context, or {@code null} if there is no service mapping for the context name.
     *
     * @param app     the application name
     * @param module  the module name
     * @param comp    the component name
     * @param context the context to check
     * @return the service name or {@code null} if there is no service
     */
    public static ServiceName serviceNameOfContext(String app, String module, String comp, String context) {
        if (context.startsWith("java:")) {
            final String namespace;
            final int i = context.indexOf('/');
            if (i == -1) {
                namespace = context.substring(5);
            } else if (i == 5) {
                // Absolute path
                return JAVA_CONTEXT_SERVICE_NAME.append(context.substring(6));
            } else {
                namespace = context.substring(5, i);
            }

            if (namespace.equals("global")) {
                return GLOBAL_CONTEXT_SERVICE_NAME.append(context.substring(12));
            } else if (namespace.equals("jboss")) {
                return JBOSS_CONTEXT_SERVICE_NAME.append(context.substring(11));
            } else if (namespace.equals("app")) {
                return contextServiceNameOfApplication(app).append(context.substring(9));
            } else if (namespace.equals("module")) {
                return contextServiceNameOfModule(app, module).append(context.substring(12));
            } else if (namespace.equals("comp")) {
                return contextServiceNameOfComponent(app, module, comp).append(context.substring(10));
            } else {
                return JBOSS_CONTEXT_SERVICE_NAME.append(context);
            }
        } else {
            return null;
        }
    }

    /**
     * Get the service name of a globally accessible context, or {@code null} if there is no service mapping for the context name.
     * <p/>
     * This will throw an exception if a non-globally accessible context is passed in.
     *
     * @param context the context to check
     * @return the service name or {@code null} if there is no service
     */
    public static ServiceName serviceNameOfGlobalEntry(String context) {
        if (context.startsWith("java:")) {
            final String namespace;
            final int i = context.indexOf('/');
            if (i == -1) {
                namespace = context.substring(5);
            } else if (i == 5) {
                // Absolute path
                return JAVA_CONTEXT_SERVICE_NAME.append(context.substring(6));
            } else {
                namespace = context.substring(5, i);
            }

            if (namespace.equals("global")) {
                return GLOBAL_CONTEXT_SERVICE_NAME.append(context.substring(12));
            } else if (namespace.equals("jboss")) {
                return JBOSS_CONTEXT_SERVICE_NAME.append(context.substring(11));
            } else if (namespace.equals("app")) {
                throw new RuntimeException("No java:app namespace is available for jndi entry " + context);
            } else if (namespace.equals("module")) {
                throw new RuntimeException("No java:module namespace is available for jndi entry " + context);
            } else if (namespace.equals("comp")) {
                throw new RuntimeException("No java:comp namespace is available for jndi entry " + context);
            } else {
                return JAVA_CONTEXT_SERVICE_NAME.append(context);
            }
        } else {
            return JAVA_CONTEXT_SERVICE_NAME.append(context);
        }
    }

    /**
     * Get the service name of a NamingStore
     *
     * @param app     the application name
     * @param module  the module name
     * @param comp    the component name
     * @param context the context to check
     * @return the service name or {@code null} if there is no service
     */
    public static ServiceName serviceNameOfNamingStore(String app, String module, String comp, String context) {
        if (context.startsWith("java:")) {
            final String namespace;
            final int i = context.indexOf('/');
            if (i == -1) {
                namespace = context.substring(5);
            } else {
                namespace = context.substring(5, i);
            }
            if (namespace.equals("global")) {
                return GLOBAL_CONTEXT_SERVICE_NAME;
            } else if (namespace.equals("jboss")) {
                return JBOSS_CONTEXT_SERVICE_NAME;
            } else if (namespace.equals("app")) {
                return contextServiceNameOfApplication(app);
            } else if (namespace.equals("module")) {
                return contextServiceNameOfModule(app, module);
            } else if (namespace.equals("comp")) {
                return contextServiceNameOfComponent(app, module, comp);
            } else {
                return JBOSS_CONTEXT_SERVICE_NAME;
            }
        } else {
            return null;
        }
    }

    /**
     * Get the service name of an environment entry
     *
     * @param app              the application name
     * @param module           the module name
     * @param comp             the component name
     * @param useCompNamespace If the component has its own comp namespace
     * @param envEntryName     The env entry name
     * @return the service name or {@code null} if there is no service
     */
    public static ServiceName serviceNameOfEnvEntry(String app, String module, String comp, boolean useCompNamespace, final String envEntryName) {
        if (envEntryName.startsWith("java:")) {
            if (useCompNamespace) {
                return serviceNameOfContext(app, module, comp, envEntryName);
            } else {
                if (envEntryName.startsWith("java:comp")) {
                    return serviceNameOfContext(app, module, module, "java:module" + envEntryName.substring("java:comp".length()));
                } else {
                    return serviceNameOfContext(app, module, module, envEntryName);
                }
            }
        } else {
            if (useCompNamespace) {
                return serviceNameOfContext(app, module, comp, "java:comp/env/" + envEntryName);
            } else {
                return serviceNameOfContext(app, module, module, "java:module/env/" + envEntryName);
            }
        }
    }

    public static class BindInfo {
        private final ServiceName parentContextServiceName;
        private final ServiceName binderServiceName;
        private final String bindName;
        // absolute jndi name inclusive of the namespace
        private final String absoluteJndiName;

        private BindInfo(final ServiceName parentContextServiceName, final String bindName) {
            this.parentContextServiceName = parentContextServiceName;
            this.binderServiceName = parentContextServiceName.append(bindName);
            this.bindName = bindName;

            this.absoluteJndiName = this.generateAbsoluteJndiName();
        }

        /**
         * The service name for the target namespace the binding will occur.
         *
         * @return The target service name
         */
        public ServiceName getParentContextServiceName() {
            return parentContextServiceName;
        }

        /**
         * The service name for binder
         *
         * @return the binder service name
         */
        public ServiceName getBinderServiceName() {
            return binderServiceName;
        }

        /**
         * The name for the binding
         *
         * @return The binding name
         */
        public String getBindName() {
            return bindName;
        }

        /**
         * Returns the absolute jndi name of this {@link BindInfo}. The absolute jndi name is inclusive of the jndi namespace
         *
         * @return
         */
        public String getAbsoluteJndiName() {
            return this.absoluteJndiName;
        }

        public String toString() {
            return "BindInfo{" +
                    "parentContextServiceName=" + parentContextServiceName +
                    ", binderServiceName=" + binderServiceName +
                    ", bindName='" + bindName + '\'' +
                    '}';
        }

        private String generateAbsoluteJndiName() {
            final StringBuffer sb = new StringBuffer();
            if (this.parentContextServiceName.equals(ContextNames.JBOSS_CONTEXT_SERVICE_NAME)) {
                sb.append("java:jboss/");
            } else if (this.parentContextServiceName.equals(ContextNames.APPLICATION_CONTEXT_NAME)) {
                sb.append("java:app/");
            } else if (this.parentContextServiceName.equals(ContextNames.MODULE_CONTEXT_SERVICE_NAME)) {
                sb.append("java:module/");
            } else if (this.parentContextServiceName.equals(ContextNames.COMPONENT_CONTEXT_SERVICE_NAME)) {
                sb.append("java:comp/");
            } else if (this.parentContextServiceName.equals(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME)) {
                sb.append("java:global/");
            } else if (this.parentContextServiceName.equals(ContextNames.JAVA_CONTEXT_SERVICE_NAME)) {
                sb.append("java:/");
            }
            sb.append(this.bindName);
            return sb.toString();
        }

    }

    /**
     * Get the service name of a NamingStore
     *
     * @param jndiName the jndi name
     * @return the bind info for the jndi name
     */
    public static BindInfo bindInfoFor(final String jndiName) {
        // TODO: handle non java: schemes
        String bindName;
        if (jndiName.startsWith("java:")) {
            bindName = jndiName.substring(5);
        } else if (!jndiName.startsWith("/")) {
            bindName = "/" + jndiName;
        } else {
            bindName = jndiName;
        }
        final ServiceName parentContextName;
        if (bindName.startsWith("jboss/")) {
            parentContextName = JBOSS_CONTEXT_SERVICE_NAME;
            bindName = bindName.substring(6);
        } else if (bindName.startsWith("global/")) {
            parentContextName = GLOBAL_CONTEXT_SERVICE_NAME;
            bindName = bindName.substring(6);
        } else if (bindName.startsWith("/")) {
            parentContextName = JAVA_CONTEXT_SERVICE_NAME;
            bindName = bindName.substring(1);
        } else {
            throw new RuntimeException("Illegal context in name: " + jndiName);
        }
        return new BindInfo(parentContextName, bindName);
    }
}
