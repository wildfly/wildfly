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
package org.jboss.as.osgi.naming;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;

import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.framework.Services;
import org.osgi.framework.BundleContext;

final class BundleContextBindingService {

    private static final String BUNDLE_CONTEXT_BINDING_NAME = "java:jboss/osgi/BundleContext";

    static ServiceController<?> addService(final ServiceTarget serviceTarget) {
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(BUNDLE_CONTEXT_BINDING_NAME);
        BinderService binderService = new BinderService(bindInfo.getBindName()) {
            @Override
            public synchronized void start(StartContext context) throws StartException {
                super.start(context);
                ServiceController<?> controller = context.getController();
                controller.setMode(Mode.ACTIVE);
            }
        };
        ServiceBuilder<?> builder = serviceTarget.addService(getBinderServiceName(), binderService);
        builder.addDependency(Services.FRAMEWORK_ACTIVE, BundleContext.class, new ManagedReferenceInjector<BundleContext>(binderService.getManagedObjectInjector()));
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new AbstractServiceListener<Object>() {
            public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                switch (transition) {
                    case STARTING_to_UP: {
                        LOGGER.infoBoundSystemContext(BUNDLE_CONTEXT_BINDING_NAME);
                        break;
                    }
                    case START_REQUESTED_to_DOWN: {
                        LOGGER.infoUnboundSystemContext(BUNDLE_CONTEXT_BINDING_NAME);
                        break;
                    }
                }
            }
        });
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    public static ServiceName getBinderServiceName() {
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(BUNDLE_CONTEXT_BINDING_NAME);
        return bindInfo.getBinderServiceName();
    }
}