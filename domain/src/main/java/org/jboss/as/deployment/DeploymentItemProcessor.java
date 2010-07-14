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

package org.jboss.as.deployment;

import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.as.deployment.item.DeploymentItemContext;
import org.jboss.as.deployment.item.DeploymentItemContextImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoaderSelector;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

/**
 * Service responsible for processing the deployment items for a deployment.  A new batch will be created for the items
 * to install services to.  All services in the batch will be given a dependency on this service in order to properly
 * setup the startup and shutdown ordering.
 *
 * @author John E. Bailey
 */
public class DeploymentItemProcessor implements Service<Void> {
    public static final ServiceName SERVICE_NAME = DeploymentService.SERVICE_NAME.append("item", "processor");

    static final Method DEPLOYMENT_MODULE_SETTER;
    static {
        try {
            DEPLOYMENT_MODULE_SETTER = DeploymentItemProcessor.class.getMethod("setModule", Module.class);
        } catch(NoSuchMethodException e) {
            throw new RuntimeException(e);  // Gross....
        }
    }

    private DeploymentUnitContextImpl deploymentUnitContext;
    private Module module;
    private DeploymentServiceListener deploymentListener;

    public DeploymentItemProcessor(DeploymentUnitContextImpl deploymentUnitContext) {
        this.deploymentUnitContext = deploymentUnitContext;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ServiceController<?> controller = context.getController();
        final ServiceContainer serviceContainer = controller.getServiceContainer();

        final DeploymentUnitContextImpl deploymentUnitContext = this.deploymentUnitContext;

        // Create batch for these items
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        if(deploymentListener != null)
            batchBuilder.addListener(deploymentListener);
        
        //  Add batch level dependency for this deployment
        batchBuilder.addDependency(controller.getName());

        final ClassLoader currentCl = getContextClassLoader();
        if(module != null) {
            setContextClassLoader(module.getClassLoader());
            DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(module.getModuleLoader());
        }
        try {
            try {
                // Process all the deployment items with the item context
                final Collection<DeploymentItem> deploymentItems = deploymentUnitContext.getDeploymentItems();
                for(DeploymentItem deploymentItem : deploymentItems) {
                    // Create a sub-batch to disable individual items from installing the batch or polluting other items service deps/listeners
                    final BatchBuilder subBatchBuilder = batchBuilder.subBatchBuilder();
                    // Construct an item context
                    final DeploymentItemContext deploymentItemContext = new DeploymentItemContextImpl(module, subBatchBuilder);
                    deploymentItem.install(deploymentItemContext);
                }
            } finally {
                DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(null);
            }
        } finally {
            setContextClassLoader(currentCl);
        }

        // Install the batch
        try {
            batchBuilder.install();
        } catch(ServiceRegistryException e) {
            throw new StartException("Failed to install deployment batch for " + deploymentUnitContext.getName(), e);
        }
    }

    private ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private void setContextClassLoader(final ClassLoader classLoader) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(classLoader);
                return null;
            }
        });
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public void setDeploymentListener(DeploymentServiceListener deploymentListener) {
        this.deploymentListener = deploymentListener;
    }
}
