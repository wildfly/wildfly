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
package org.jboss.as.arquillian.container;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.arquillian.protocol.jmx.JMXTestRunnerMBean;
import org.jboss.arquillian.protocol.jmx.RepositoryArchiveLocator;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.DeployableContainer;
import org.jboss.arquillian.spi.DeploymentException;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.as.arquillian.protocol.servlet.ServletMethodExecutor;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.modules.management.ObjectProperties;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.osgi.jmx.MBeanProxy;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * A JBossAS deployable container
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public abstract class AbstractDeployableContainer implements DeployableContainer {

    static final ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("jboss.msc", ObjectProperties.properties(ObjectProperties.property("type", "container"),
                    ObjectProperties.property("name", "jbossas")));
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Logger log = Logger.getLogger(AbstractDeployableContainer.class.getName());

    private JBossAsContainerConfiguration containerConfig;
    private ModelControllerClient modelControllerClient;
    private ServerDeploymentManager deploymentManager;

    private final Map<Archive<?>, String> registry = new HashMap<Archive<?>, String>();

    @Override
    public void setup(Context context, Configuration configuration) {
        containerConfig = configuration.getContainerConfig(JBossAsContainerConfiguration.class);
        modelControllerClient = ModelControllerClient.Factory.create(containerConfig.getBindAddress(), containerConfig.getManagementPort());
        deploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);
    }

    @Override
    public final void start(Context context) throws LifecycleException {
        startInternal(context);
        try {
            MBeanServerConnection mbeanServer = getMBeanServerConnection(10000);
            boolean mbeanAvailable = mbeanServer.isRegistered(JMXTestRunnerMBean.OBJECT_NAME);
            if (mbeanAvailable == false) {
                String asVersion = AbstractDeployableContainer.class.getPackage().getImplementationVersion();
                deployMavenArtifact("org.jboss.as", "jboss-as-arquillian-service", asVersion);
                waitForMBean(JMXTestRunnerMBean.OBJECT_NAME, 5000);
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot deploy arquilllian service", ex);
        }
    }

    protected abstract void startInternal(Context context) throws LifecycleException;

    @Override
    public final void stop(Context context) throws LifecycleException {
        if (registry.containsValue("jboss-as-arquillian-service")) {
            undeployMavenArtifact("jboss-as-arquillian-service");
        }
        stopInternal(context);
    }

    protected abstract void stopInternal(Context context) throws LifecycleException;

    protected JBossAsContainerConfiguration getContainerConfiguration() {
        return containerConfig;
    }

    protected ModelControllerClient getModelControllerClient() {
        return modelControllerClient;
    }

    @Override
    public ContainerMethodExecutor deploy(Context context, Archive<?> archive) throws DeploymentException {
        try {
            InputStream input = archive.as(ZipExporter.class).exportZip();
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(archive.getName(), input).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();
            executeDeploymentPlan(plan, deployAction, archive);

            return getContainerMethodExecutor(context);
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

    protected MBeanServerConnection getMBeanServerConnection(long timeout) {
        while (timeout > 0) {
            try {
                return getMBeanServerConnection();
            } catch (Exception ex) {
                // ignore
            }
            try {
                Thread.sleep(100);
                timeout -= 100;
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        throw new IllegalStateException("MBeanServerConnection not available");
    }

    protected void waitForMBean(ObjectName objectName, long timeout) {
        boolean mbeanAvailable = false;
        MBeanServerConnection mbeanServer = getMBeanServerConnection(timeout);
        while (timeout > 0 && mbeanAvailable == false) {
            try {
                mbeanAvailable = mbeanServer.isRegistered(objectName);
            } catch (Exception ex) {
                // ignore
            }
            if (mbeanAvailable == false) {
                try {
                    Thread.sleep(100);
                    timeout -= 100;
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }
        if (mbeanAvailable == false)
            throw new IllegalStateException("MBean not available: " + objectName);
    }

    protected void waitForServiceState(ServiceName serviceName, State expectedState, long timeout) {

        ObjectName objectName = OBJECT_NAME;
        MBeanServerConnection mbeanServer = getMBeanServerConnection();
        ServiceContainerMXBean proxy = MBeanProxy.get(mbeanServer, objectName, ServiceContainerMXBean.class);

        State currentState = State.valueOf(proxy.getServiceStatus(serviceName.getCanonicalName()).getStateName());
        while (timeout > 0 && currentState != expectedState) {
            try {
                // TODO: Change this to use mbean notifications
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
            timeout -= 100;
            currentState = State.valueOf(proxy.getServiceStatus(serviceName.getCanonicalName()).getStateName());
        }
        if (currentState != expectedState)
            throw new IllegalStateException("Unexpected state for [" + serviceName + "] - " + currentState);
    }

    protected ContainerMethodExecutor getContainerMethodExecutor(Context context) {
        JBossAsContainerConfiguration config = context.get(Configuration.class).getContainerConfig(JBossAsContainerConfiguration.class);
        if (config.isExecuteWithServlet()) {
            try {
                return new ServletMethodExecutor(new URL("http", config.getBindAddress().getHostName(), config.getHttpPort(), "/"));
            } catch (MalformedURLException e) {
                // AutoGenerated
                throw new RuntimeException(e);
            }
        } else {
            return getContainerMethodExecutor();
        }
    }

    protected abstract MBeanServerConnection getMBeanServerConnection();

    protected abstract ContainerMethodExecutor getContainerMethodExecutor();

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction, Archive<?> archive) throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        registry.put(archive, deployAction.getDeploymentUnitUniqueName());
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }

        return deployAction.getDeploymentUnitUniqueName();
    }

    private void deployMavenArtifact(String groupId, String artifactId, String version) throws DeploymentException {
        String filespec = groupId + ":" + artifactId + ":jar:" + version;
        URL artifactURL = RepositoryArchiveLocator.getArtifactURL(groupId, artifactId, version);
        if (artifactURL == null)
            throw new DeploymentException("Cannot obtain maven artifact: " + filespec);

        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(artifactId, artifactURL).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();
            executeDeploymentPlan(plan, deployAction, ShrinkWrap.create(JavaArchive.class, artifactId));
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    private void undeployMavenArtifact(String artifactId) {
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            DeploymentPlan plan = builder.undeploy(artifactId).remove(artifactId).build();
            Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
            future.get();
        } catch (Exception ex) {
            log.warning("Cannot undeploy: " + artifactId + ":" + ex.getMessage());
        }
    }
}