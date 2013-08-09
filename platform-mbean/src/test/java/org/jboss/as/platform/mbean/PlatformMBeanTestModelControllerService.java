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

package org.jboss.as.platform.mbean;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * {@code Service<ModelController>} implementation for use in platform mbean resource tests.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PlatformMBeanTestModelControllerService extends AbstractControllerService {

    private static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode().set("Test");
        }
    };

    final CountDownLatch latch = new CountDownLatch(2);

    /**
     * Construct a new instance.
     *
     */
    protected PlatformMBeanTestModelControllerService() {
        super(ProcessType.EMBEDDED_SERVER, new RunningModeControl(RunningMode.NORMAL), new NullConfigurationPersister(), new ControlledProcessState(true),
        ResourceBuilder.Factory.create(PathElement.pathElement("root"),new NonResolvingResourceDescriptionResolver()).build(), null, ExpressionResolver.TEST_RESOLVER, AuditLogger.NO_OP_LOGGER);
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {

        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        rootRegistration.registerOperationHandler(ValidateAddressOperationHandler.DEFINITION, ValidateAddressOperationHandler.INSTANCE);

        // Platform mbeans
        PlatformMBeanResourceRegistrar.registerPlatformMBeanResources(rootRegistration);
        rootResource.registerChild(PlatformMBeanConstants.ROOT_PATH, new RootPlatformMBeanResource());
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        latch.countDown();
    }

    @Override
    protected void bootThreadDone() {
        super.bootThreadDone();
        latch.countDown();
    }
}
