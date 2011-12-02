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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.Locale;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.clustering.infinispan.InfinispanLogger.ROOT_LOGGER;

/**
 * @author Paul Ferraro
 */
public class InfinispanSubsystemAdd extends AbstractAddStepHandler implements DescriptionProvider {

    private static final Logger log = Logger.getLogger(InfinispanSubsystemAdd.class.getPackage().getName());

    static ModelNode createOperation(ModelNode address, ModelNode existing) {
        ModelNode operation = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);
        populate(existing, operation);
        return operation;
    }

    private static void populate(ModelNode source, ModelNode target) {
        target.get(ModelKeys.DEFAULT_CACHE_CONTAINER).set(source.require(ModelKeys.DEFAULT_CACHE_CONTAINER));
        target.get(ModelKeys.CACHE_CONTAINER).setEmptyObject();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.as.controller.descriptions.DescriptionProvider#getModelDescription(java.util.Locale)
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getSubsystemAddDescription(locale);
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        populate(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        ROOT_LOGGER.activatingSubsystem();
        ServiceTarget target = context.getServiceTarget();
        newControllers.add(target.addService(EmbeddedCacheManagerDefaultsService.SERVICE_NAME, new EmbeddedCacheManagerDefaultsService())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());
        String defaultContainer = operation.require(ModelKeys.DEFAULT_CACHE_CONTAINER).asString();
        InjectedValue<EmbeddedCacheManager> container = new InjectedValue<EmbeddedCacheManager>();
        ValueService<EmbeddedCacheManager> service = new ValueService<EmbeddedCacheManager>(container);
        newControllers.add(target.addService(EmbeddedCacheManagerService.getServiceName(null), service)
                .addDependency(EmbeddedCacheManagerService.getServiceName(defaultContainer), EmbeddedCacheManager.class, container)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
