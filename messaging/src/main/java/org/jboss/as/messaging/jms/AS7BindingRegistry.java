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

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.hornetq.spi.core.naming.BindingRegistry;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jboss.as.messaging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

/**
 * A {@link BindingRegistry} implementation for JBoss AS7.
 *
 * @author Jason T. Greene
 * @author Jaikiran Pai
 */
public class AS7BindingRegistry implements BindingRegistry {

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
            throw MESSAGES.cannotBindJndiName();
        }
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        container.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue(obj)))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
        ROOT_LOGGER.boundJndiName(bindInfo.getAbsoluteJndiName());
        return true;
    }

    /**
     * Unbind the resource and wait until the corresponding binding service is effectively removed.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void unbind(String name) {
        if (name == null || name.isEmpty()) {
            throw MESSAGES.cannotUnbindJndiName();
        }
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
        ServiceController<?> bindingService = container.getService(bindInfo.getBinderServiceName());
        if (bindingService == null) {
            ROOT_LOGGER.debugf("Cannot unbind %s since no binding exists with that name", name);
            return;
        }
        final CountDownLatch removedLatch = new CountDownLatch(1);
        final ServiceListener listener = new AbstractServiceListener() {
            @Override
            public void transition(ServiceController controller, Transition transition) {
                if (transition.getAfter() == ServiceController.Substate.REMOVED) {
                    removedLatch.countDown();
                }
            }
        };
        bindingService.addListener(listener);

        // remove the binding service
        bindingService.setMode(ServiceController.Mode.REMOVE);

        try {
            if (!removedLatch.await(5, SECONDS)) {
                ROOT_LOGGER.failedToUnbindJndiName(name, 5, SECONDS.toString().toLowerCase(Locale.US));
                return;
            }
            ROOT_LOGGER.unboundJndiName(bindInfo.getAbsoluteJndiName());
        } catch (InterruptedException e) {
            ROOT_LOGGER.failedToUnbindJndiName(name, 5, SECONDS.toString().toLowerCase(Locale.US));
        } finally {
            bindingService.removeListener(listener);
        }
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
                throw MESSAGES.nullVar("contextServiceName");
            }
            if (relativeJndiName == null) {
                throw MESSAGES.nullVar("relativeJndiName");
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
            } else if (this.jndiContextServiceName.equals(ContextNames.APPLICATION_CONTEXT_SERVICE_NAME)) {
                sb.append("java:app/");
            } else if (this.jndiContextServiceName.equals(ContextNames.MODULE_CONTEXT_SERVICE_NAME)) {
                sb.append("java:module/");
            } else if (this.jndiContextServiceName.equals(ContextNames.COMPONENT_CONTEXT_SERVICE_NAME)) {
                sb.append("java:comp/");
            } else if (this.jndiContextServiceName.equals(ContextNames.GLOBAL_CONTEXT_SERVICE_NAME)) {
                sb.append("java:global/");
            }
            sb.append(this.relativeJndiName);
            return sb.toString();
        }
    }
}
