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

package org.jboss.as.controller.test;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractControllerTestBase {

    abstract DescriptionProvider getRootDescriptionProvider();
    abstract void initModel(final ModelNodeRegistration registration);
    abstract ModelNode createCoreModel();

    private ServiceContainer container;
    private NewModelController controller;

    public NewModelController getController() {
        return controller;
    }

    @Before
    public void setupController() throws InterruptedException {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        ModelControllerService svc = new ModelControllerService(container, processState);
        ServiceBuilder<NewModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await();
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
    }

    class ModelControllerService extends AbstractControllerService {

        private final CountDownLatch latch = new CountDownLatch(1);

        ModelControllerService(final ServiceContainer serviceContainer, final ControlledProcessState processState) {
            super(NewOperationContext.Type.SERVER, new NullConfigurationPersister(), processState, getRootDescriptionProvider(), null);
        }

        @Override
        public void start(StartContext context) throws StartException {
            try {
                super.start(context);
            } finally {
                latch.countDown();
            }
        }

        protected ModelNode createCoreModel() {
            return AbstractControllerTestBase.this.createCoreModel();
        }

        protected void initModel(ModelNodeRegistration rootRegistration) {
            try {
                AbstractControllerTestBase.this.initModel(rootRegistration);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


}
