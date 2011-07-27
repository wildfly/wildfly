/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import org.hornetq.spi.core.naming.BindingRegistry;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

/**
 * A {@link BindingRegistry} implementation for JBoss AS7.
 *
 * @author Jason T. Greene
 * @author Jaikiran Pai
 */
public class AS7BindingRegistry implements BindingRegistry {

    private static final Logger logger = Logger.getLogger(AS7BindingRegistry.class);

    private final ServiceContainer container;

    public AS7BindingRegistry(ServiceContainer container) {
        this.container = container;
    }

    @Override
    public Object getContext() {
        // NOOP
        return null;
    }

    @Override
    public void setContext(Object ctx) {
        // NOOP
    }

    @Override
    public Object lookup(String name) {
        // NOOP
        return null;
    }

    @Override
    public boolean bind(String name, Object obj) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Cannot bind a null or empty string as jndi name");
        }
        if (name.equals("java:jboss/") || name.equals("java:comp/") || name.equals("java:module/")
                || name.equals("java:/app") || name.equals("java:global/") || name.equals("java:/")) {
            throw new IllegalArgumentException("Missing relative path in (invalid) jndi name: " + name);
        }
        final JndiBinding jndiBinding = JndiBinding.parse(name);
        if (jndiBinding == null) {
            throw new IllegalArgumentException("Binding to " + name + " isn't allowed, since it belongs to a unknown/unsupported jndi name context");
        }
        // create the binding service
        final BinderService binderService = new BinderService(name);
        container.addService(jndiBinding.jndiContextServiceName.append(jndiBinding.relativeJndiName), binderService)
                .addDependency(jndiBinding.jndiContextServiceName, NamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue(obj)))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        logger.info("Bound messaging object to jndi name " + jndiBinding);
        return true;
    }

    @Override
    public void unbind(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Cannot unbind null or empty jndi name");
        }
        final JndiBinding jndiBinding = JndiBinding.parse(name);
        if (jndiBinding == null) {
            throw new IllegalArgumentException("Cannot unbind " + name + " since it belongs to a unknown/unsupported jndi name context");
        }
        ServiceController<?> bindingService = container.getService(jndiBinding.jndiContextServiceName.append(jndiBinding.relativeJndiName));
        if (bindingService == null) {
            logger.debug("Cannot unbind " + name + " since no binding exists with that name");
            return;
        }
        // remove the binding service
        bindingService.setMode(ServiceController.Mode.REMOVE);
    }

    @Override
    public void close() {
        // NOOP
    }

    /**
     * Utility class holding a jndi context {@link ServiceName} and a jndi name relative to that jndi context
     */
    private static class JndiBinding {

        /**
         * Jndi name relative to the jndi context {@link #jndiContextServiceName}
         */
        private final String relativeJndiName;

        /**
         * The ServiceName of the jndi context
         */
        private final ServiceName jndiContextServiceName;

        private final String cachedToString;

        JndiBinding(final ServiceName contextServiceName, final String relativeJndiName) {
            if (contextServiceName == null) {
                throw new IllegalArgumentException("ServiceName of jndi context cannot be null");
            }
            if (relativeJndiName == null) {
                throw new IllegalArgumentException("Relative jndi name cannot be null");
            }
            this.jndiContextServiceName = contextServiceName;
            this.relativeJndiName = relativeJndiName;

            this.cachedToString = this.generateToString();
        }

        /**
         * Creates a {@link JndiBinding} out of the passed <code>name</code>.
         * <p/>
         * If the passed jndi name doesn't start with java: namespace, then it is considered relative to java:jboss/jms
         * namespace and a {@link JndiBinding} corresponding to that namespace is returned.
         * <p/>
         * If the passed jndi name starts with java: namespace, but doesn't belong to known/supported jndi context, then
         * this method returns null. Known/Supported jndi context include java:jboss/, java:global/, java:app/, java:module/,
         * java:comp/, java:/
         *
         * @param name The jndi name to parse
         * @return
         */
        static JndiBinding parse(final String name) {
            String relativeJndiName = null;
            if (name.startsWith("java:jboss/")) { // java:jboss/<something>
                relativeJndiName = name.substring(11);
                return new JndiBinding(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, relativeJndiName);
            }
            if (name.startsWith("java:comp/")) { // java:comp/<something>
                relativeJndiName = name.substring(10);
                return new JndiBinding(ContextNames.COMPONENT_CONTEXT_SERVICE_NAME, relativeJndiName);
            }
            if (name.startsWith("java:module/")) { // java:module/<something>
                relativeJndiName = name.substring(12);
                return new JndiBinding(ContextNames.MODULE_CONTEXT_SERVICE_NAME, relativeJndiName);
            }
            if (name.startsWith("java:app/")) { // java:app/<something>
                relativeJndiName = name.substring(9);
                return new JndiBinding(ContextNames.APPLICATION_CONTEXT_SERVICE_NAME, relativeJndiName);
            }
            if (name.startsWith("java:global/")) { // java:global/<something>
                relativeJndiName = name.substring(12);
                return new JndiBinding(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME, relativeJndiName);
            }
            if (name.startsWith("java:/")) { // java:/<something>
                relativeJndiName = name.substring(6);
                return new JndiBinding(ContextNames.JAVA_CONTEXT_SERVICE_NAME, relativeJndiName);
            }
            if (name.startsWith("java:")) { // java:<something> (Note that this is *not* the same as java:/<something>.
                // we don't allow java:<something>
                return null;
            }
            // no java: namespace, so consider this relative to java:jboss/jms/ (by default)
            relativeJndiName = name;
            return new JndiBinding(ContextNames.JBOSS_CONTEXT_SERVICE_NAME, "jms/" + relativeJndiName);

        }

        @Override
        public String toString() {
            return this.cachedToString;
        }

        private String generateToString() {
            final StringBuffer sb = new StringBuffer();
            if (this.jndiContextServiceName.equals(ContextNames.JBOSS_CONTEXT_SERVICE_NAME)) {
                sb.append("java:jboss/");
            } else if (this.jndiContextServiceName.equals(ContextNames.APPLICATION_CONTEXT_NAME)) {
                sb.append("java:app/");
            } else if (this.jndiContextServiceName.equals(ContextNames.MODULE_CONTEXT_SERVICE_NAME)) {
                sb.append("java:module/");
            } else if (this.jndiContextServiceName.equals(ContextNames.COMPONENT_CONTEXT_SERVICE_NAME)) {
                sb.append("java:comp/");
            } else if (this.jndiContextServiceName.equals(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME)) {
                sb.append("java:global/");
            } else if (this.jndiContextServiceName.equals(ContextNames.JAVA_CONTEXT_SERVICE_NAME)) {
                sb.append("java:/");
            }
            sb.append(this.relativeJndiName);
            return sb.toString();
        }
    }
}
