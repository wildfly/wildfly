/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.as.naming.deployment.ContextNames.BindInfo;

import javax.naming.InitialContext;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Values;

/**
 * Utility class to install BinderService (either to bind actual objects or create alias on another binding).
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2013 Red Hat Inc.
 */
public class BinderServiceUtil {

    /**
     * Install a binder service to bind the {@code obj} using the binding {@code name}.

     * @param serviceTarget
     * @param name the binding name
     * @param obj the object that must be bound
     */
    public static void installBinderService(final ServiceTarget serviceTarget,
                                                 final String name,
                                                 final Object obj) {
        final BindInfo bindInfo = ContextNames.bindInfoFor(name);
        final BinderService binderService = new BinderService(bindInfo.getBindName());

        serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(Values.immediateValue(obj)))
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    /**
     * Install a binder service to bind the value of the {@code service} using the binding {@code name}.

     * @param serviceTarget
     * @param name the binding name
     * @param service the service whose value must be bound
     */
    public static void installBinderService(final ServiceTarget serviceTarget,
                                            final String name,
                                            final Service<?> service,
                                            final ServiceName... dependencies) {
        final BindInfo bindInfo = ContextNames.bindInfoFor(name);
        final BinderService binderService = new BinderService(bindInfo.getBindName());

        final ServiceBuilder serviceBuilder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector())
                .addInjection(binderService.getManagedObjectInjector(), new ValueManagedReferenceFactory(service))
                // we set it in passive mode so that missing dependencies (which is possible/valid when it's a backup HornetQ server and the services
                // haven't been activated on it due to the presence of a different live server) don't cause jms-topic/jms-queue add operations
                // to fail
                .setInitialMode(ServiceController.Mode.PASSIVE);
        if (dependencies != null && dependencies.length > 0) {
            serviceBuilder.addDependencies(dependencies);
        }
        serviceBuilder.install();
    }

    public static void installAliasBinderService(final ServiceTarget serviceTarget,
                                                 final BindInfo bindInfo,
                                                 final String alias) {
        final BindInfo aliasBindInfo = ContextNames.bindInfoFor(alias);

        final BinderService aliasBinderService = new BinderService(alias);
        aliasBinderService.getManagedObjectInjector().inject(new AliasManagedReferenceFactory(bindInfo.getAbsoluteJndiName()));

        serviceTarget.addService(aliasBindInfo.getBinderServiceName(), aliasBinderService)
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, aliasBinderService.getNamingStoreInjector())
                .addDependency(bindInfo.getBinderServiceName())
                .addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                ROOT_LOGGER.boundJndiName(alias);
                                break;
                            }
                            case DOWN: {
                                ROOT_LOGGER.unboundJndiName(alias);
                                break;
                            }
                            case REMOVED: {
                                ROOT_LOGGER.debugf("Removed messaging object [%s]", alias);
                                break;
                            }
                        }
                    }
                })
                .install();
    }

    private static final class AliasManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

        private final String name;

        /**
         * @param name original JNDI name
         */
        public AliasManagedReferenceFactory(String name) {
            this.name = name;
        }

        @Override
        public ManagedReference getReference() {
            try {
                final Object value = new InitialContext().lookup(name);
                return new ValueManagedReference(new ImmediateValue<Object>(value));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getInstanceClassName() {
            final Object value = getReference().getInstance();
            return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
        }

        @Override
        public String getJndiViewInstanceValue() {
            return String.valueOf(getReference().getInstance());
        }
    }
}