/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.osgi.service;

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.osgi.management.OperationAssociation;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleLifecyclePlugin;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.IntegrationService;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.vfs.VFSUtils;
import org.osgi.framework.BundleException;

/**
 * An {@link IntegrationService} that that handles the bundle lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Nov-2010
 */
public final class BundleLifecycleIntegration implements BundleLifecyclePlugin, IntegrationService<BundleLifecyclePlugin> {

    private static Map<String, Deployment> deploymentMap = new HashMap<String, Deployment>();

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private ServerDeploymentManager deploymentManager;

    @Override
    public ServiceName getServiceName() {
        return BUNDLE_LIFECYCLE_PLUGIN;
    }

    @Override
    public ServiceController<BundleLifecyclePlugin> install(ServiceTarget serviceTarget) {
        ServiceBuilder<BundleLifecyclePlugin> builder = serviceTarget.addService(getServiceName(), this);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, injectedController);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Starting: %s in mode %s", controller.getName(), controller.getMode());
        deploymentManager = new ModelControllerServerDeploymentManager(injectedController.getValue());
    }

    @Override
    public void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.tracef("Stopping: %s in mode %s", controller.getName(), controller.getMode());
    }

    @Override
    public BundleLifecyclePlugin getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public static Deployment getDeployment(String contextName) {
        return deploymentMap.get(contextName);
    }

    public static Deployment removeDeployment(String contextName) {
        return deploymentMap.remove(contextName);
    }

    @Override
    public void install(final Deployment dep, final DefaultHandler handler) throws BundleException {
        // Do the install directly if we have a running management op
        // https://issues.jboss.org/browse/AS7-5642
        if (OperationAssociation.INSTANCE.getAssociation() != null) {
            LOGGER.warnCannotDeployBundleFromManagementOperation(dep);
            BundleManager bundleManager = injectedBundleManager.getValue();
            bundleManager.installBundle(dep, null);
        } else {
            LOGGER.debugf("Install deployment: %s", dep);
            executeDeployOperation(dep);
        }
    }

    private void executeDeployOperation(final Deployment dep) throws BundleException {
        String contextName = getContextName(dep);
        deploymentMap.put(contextName, dep);
        try {
            InputStream input = dep.getRoot().openStream();
            try {
                ServerDeploymentHelper server = new ServerDeploymentHelper(deploymentManager);
                server.deploy(contextName, input);
            } finally {
                VFSUtils.safeClose(input);
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw MESSAGES.cannotDeployBundle(ex, dep);
        }
    }

    @Override
    public void start(XBundle bundle, int options, DefaultHandler handler) throws BundleException {
        handler.start(bundle, options);
    }

    @Override
    public void stop(XBundle bundle, int options, DefaultHandler handler) throws BundleException {
        handler.stop(bundle, options);
    }

    @Override
    public void uninstall(XBundle bundle, DefaultHandler handler) {
        LOGGER.tracef("Uninstall deployment: %s", bundle);
        try {
            ServerDeploymentHelper server = new ServerDeploymentHelper(deploymentManager);
            Deployment dep = bundle.adapt(Deployment.class);
            server.undeploy(getContextName(dep));
        } catch (Exception ex) {
            LOGGER.warnCannotUndeployBundle(ex, bundle);
        }
    }

    private String getContextName(Deployment dep) {
        String name = dep.getLocation();
        if (name.endsWith("/"))
            name = name.substring(0, name.length() - 1);
        int idx = name.lastIndexOf("/");
        if (idx > 0)
            name = name.substring(idx + 1);
        return name;
    }
}
