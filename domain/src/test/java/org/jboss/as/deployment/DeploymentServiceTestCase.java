/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment;

import org.jboss.as.deployment.module.DeploymentModuleLoaderImpl;
import org.jboss.as.deployment.unit.DeploymentChainImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author John E. Bailey
 */
public class DeploymentServiceTestCase {
    private static final ServiceName serviceName = ServiceName.of("testDeployment");
    private DeploymentService deploymentService = new DeploymentService("testDeployment");
    private ServiceController serviceController;
    private ServiceContainer serviceContainer;
    private StartContext startContext;

    @Before
    public void setup() throws Exception {
        deploymentService.setDeploymentChain(new DeploymentChainImpl("test.chain"));
        deploymentService.setDeploymentModuleLoader(new DeploymentModuleLoaderImpl(null));

        serviceContainer = ServiceContainer.Factory.create();
        serviceController = new MockServiceController(serviceContainer, serviceName);
        startContext = new StartContext() {
            @Override
            public void failed(StartException reason) throws IllegalStateException {
            }

            @Override
            public void asynchronous() throws IllegalStateException {
            }

            @Override
            public void complete() throws IllegalStateException {
            }

            @Override
            public ServiceController<?> getController() {
                return serviceController;
            }
        };
    }

    @After
    public void shutdown() {
        serviceContainer.shutdown();
    }

    @Test
    public void testAssertRequiredParams() throws Exception {
        DeploymentService deploymentService = new DeploymentService("test");
        try {
            deploymentService.start(startContext);
            fail("Should have thrown StartException");
        } catch(StartException expected){}
        deploymentService.setDeploymentChain(new DeploymentChainImpl("test.chain"));
        try {
            deploymentService.start(startContext);
            fail("Should have thrown StartException");
        } catch(StartException expected){}
        deploymentService.setDeploymentModuleLoader(new DeploymentModuleLoaderImpl(null));
        try {
            deploymentService.start(startContext);
            fail("Should have thrown StartException");
        } catch(StartException expected){}
        deploymentService.setDeploymentRoot(VFS.getChild("testRoot"));
//        try {
//            deploymentService.start(startContext);
//        } catch(StartException e) {
//            fail("Should have thrown StartException");
//        }
    }

    @Test
    public void testStartDeployment() throws Exception {
        //deploymentService.start(startContext);
    }

    private static class MockServiceController implements ServiceController {
        private final ServiceContainer serviceContainer;
        private final ServiceName serviceName;

        private MockServiceController(ServiceContainer serviceContainer, ServiceName serviceName) {
            this.serviceContainer = serviceContainer;
            this.serviceName = serviceName;
        }

        @Override
        public ServiceContainer getServiceContainer() {
            return serviceContainer;
        }

        @Override
        public Mode getMode() {
            return Mode.AUTOMATIC;  
        }

        @Override
        public void setMode(Mode mode) {
        }

        @Override
        public State getState() {
            return State.STARTING;
        }

        @Override
        public Object getValue() throws IllegalStateException {
            return null;
        }

        @Override
        public ServiceName getName() {
            return serviceName;
        }

        @Override
        public void addListener(ServiceListener serviceListener) {
        }

        @Override
        public void removeListener(ServiceListener serviceListener) {
        }

        @Override
        public void remove() throws IllegalStateException {
        }

        @Override
        public StartException getStartException() {
            return null; 
        }

        @Override
        public void retry() {
        }
    }
}
