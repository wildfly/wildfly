/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.server.client.api.deployment.DeploymentAction;
import org.jboss.as.server.client.api.deployment.DeploymentPlan;
import org.jboss.as.server.client.api.deployment.DeploymentPlanBuilder;
import org.jboss.as.server.client.api.deployment.ServerDeploymentActionResult;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.modules.management.ObjectProperties;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.osgi.jmx.MBeanProxy;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * A JBossAS server connector
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public abstract class AbstractDeployableContainer implements DeployableContainer {

    static final ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("jboss.msc", ObjectProperties.properties(ObjectProperties.property("type", "container"), ObjectProperties.property("name", "jbossas")));
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Logger log = Logger.getLogger(AbstractDeployableContainer.class.getName());

    private JBossAsContainerConfiguration containerConfig;
    private ServerDeploymentManager deploymentManager;

    private final Map<Archive<?>, String> registry = new HashMap<Archive<?>, String>();

    @Override
    public void setup(Context context, Configuration configuration) {
        containerConfig = configuration.getContainerConfig(JBossAsContainerConfiguration.class);
        ModelControllerClient client = ModelControllerClient.Factory.create(containerConfig.getBindAddress(), containerConfig.getManagementPort());
        deploymentManager = ServerDeploymentManager.Factory.create(client);
    }

    protected JBossAsContainerConfiguration getContainerConfiguration() {
        return containerConfig;
    }

    @Override
    public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException {
        try {
            // If this is an OSGi archive
            if (BundleInfo.isValidateBundleManifest(ManifestUtils.getManifest(archive, false)))
                startOSGiSubsystem();

            InputStream input = archive.as(ZipExporter.class).exportZip();
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(archive.getName(), input).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();
            String runtimeName = executeDeploymentPlan(plan, deployAction);
            registry.put(archive, runtimeName);

            return getContainerMethodExecutor();
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    @Override
    public void undeploy(Context context, Archive<?> archive) throws DeploymentException {
        String runtimeName = registry.remove(archive);
        if (runtimeName != null) {
            try {
                DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
                DeploymentPlan plan = builder.undeploy(runtimeName).remove(runtimeName).build();
                Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
                future.get();
            } catch (Exception ex) {
                log.warning("Cannot undeploy: " + runtimeName + ":" + ex.getMessage());
            }
        }
    }

    protected void waitForMBean(ObjectName objectName, long timeout) throws IOException, InterruptedException {
        boolean mbeanAvailable = false;
        MBeanServerConnection mbeanServer = null;
        while (timeout > 0 && mbeanAvailable == false) {
            if (mbeanServer == null) {
                try {
                    mbeanServer = getMBeanServerConnection();
                } catch (Exception ex) {
                    // ignore
                }
            }
            mbeanAvailable = (mbeanServer != null && mbeanServer.isRegistered(objectName));
            Thread.sleep(100);
            timeout -= 100;
        }
        if (mbeanAvailable == false)
            throw new IllegalStateException("MBean not available: " + objectName);
    }

    protected void waitForServiceState(ServiceName serviceName, State expectedState, long timeout) throws IOException, InterruptedException {

        ObjectName objectName = OBJECT_NAME;
        MBeanServerConnection mbeanServer = getMBeanServerConnection();
        ServiceContainerMXBean proxy = MBeanProxy.get(mbeanServer, objectName, ServiceContainerMXBean.class);

        State currentState = State.valueOf(proxy.getServiceStatus(serviceName.getCanonicalName()).getStateName());
        while (timeout > 0 && currentState != expectedState) {
            // TODO: Change this to use mbean notifications
            Thread.sleep(100);
            timeout -= 100;
            currentState = State.valueOf(proxy.getServiceStatus(serviceName.getCanonicalName()).getStateName());
        }
        if (currentState != expectedState)
            throw new IllegalStateException("Unexpected state for [" + serviceName + "] - " + currentState);
    }

    protected abstract MBeanServerConnection getMBeanServerConnection();

    protected abstract ContainerMethodExecutor getContainerMethodExecutor();

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        // FIXME Evil hack, remove when we have update results blocking on all deployment services
        Thread.sleep(2000);

        return deployAction.getDeploymentUnitUniqueName();
    }

    private void startOSGiSubsystem() throws IOException, InterruptedException {

        ObjectName objectName = OBJECT_NAME;
        waitForMBean(objectName, 5000);

        MBeanServerConnection mbeanServer = getMBeanServerConnection();
        ServiceContainerMXBean proxy = MBeanProxy.get(mbeanServer, objectName, ServiceContainerMXBean.class);
        ServiceName serviceName = ServiceName.JBOSS.append("osgi", "context");
        if (State.valueOf(proxy.getServiceStatus(serviceName.getCanonicalName()).getStateName()) != State.UP) {
            proxy.setServiceMode(serviceName.getCanonicalName(), Mode.ACTIVE.toString());
            waitForServiceState(serviceName, State.UP, 5000);
        }
    }
}