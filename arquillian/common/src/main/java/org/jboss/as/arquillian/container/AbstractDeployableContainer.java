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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.protocol.jmx.JMXTestRunnerMBean;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.DeploymentAction;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlan;
import org.jboss.as.controller.client.helpers.standalone.DeploymentPlanBuilder;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentActionResult;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentPlanResult;
import org.jboss.as.jmx.ObjectNameFactory;
import org.jboss.modules.management.ObjectProperties;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.jboss.shrinkwrap.resolver.api.maven.filter.StrictFilter;

/**
 * A JBossAS deployable container
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public abstract class AbstractDeployableContainer<T extends JBossAsCommonConfiguration> implements DeployableContainer<T> {

    protected static final ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("jboss.msc", ObjectProperties.properties(
                    ObjectProperties.property("type", "container"), ObjectProperties.property("name", "jboss-as")));
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final Logger log = Logger.getLogger(AbstractDeployableContainer.class.getName());

    private T containerConfig;
    private ModelControllerClient modelControllerClient;
    private ServerDeploymentManager deploymentManager;

    private final Map<Object, String> registry = new HashMap<Object, String>();

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("jmx-as7");
    }

    @Override
    public void setup(T config) {
        containerConfig = config;
        modelControllerClient = ModelControllerClient.Factory.create(config.getBindAddress(), config.getManagementPort());
        deploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);
        modelControllerClient = ModelControllerClient.Factory.create(config.getBindAddress(), config.getManagementPort());
        deploymentManager = ServerDeploymentManager.Factory.create(modelControllerClient);
    }

    @Override
    public final void start() throws LifecycleException {
        startInternal();
        try {
            MBeanServerConnection mbeanServer = getMBeanServerConnection();
            ObjectName objectName = ObjectNameFactory.create(JMXTestRunnerMBean.OBJECT_NAME);
            boolean mbeanAvailable = mbeanServer.isRegistered(objectName);
            if (mbeanAvailable == false) {
                String asVersion = AbstractDeployableContainer.class.getPackage().getImplementationVersion();
                asVersion = asVersion != null ? asVersion : System.getProperty("project.version");
                deployMavenArtifact("org.jboss.as", "jboss-as-arquillian-service", asVersion);
                waitForMBean(objectName, 5000);
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot deploy arquilllian service", ex);
        }
    }

    protected abstract void startInternal() throws LifecycleException;

    @Override
    public final void stop() throws LifecycleException {
        if (registry.containsValue("jboss-as-arquillian-service")) {
            undeployMavenArtifact("jboss-as-arquillian-service");
        }
        stopInternal();
    }

    protected abstract void stopInternal() throws LifecycleException;

    protected T getContainerConfiguration() {
        return containerConfig;
    }

    protected ModelControllerClient getModelControllerClient() {
        return modelControllerClient;
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        deploy(descriptor.getDescriptorName(), descriptor, new ByteArrayInputStream(descriptor.exportAsString().getBytes()));
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        undeploy((Object) descriptor);
    }

    private String deploy(String deploymentName, Object deployment, InputStream content) throws DeploymentException {
        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(deploymentName, content).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();

            return executeDeploymentPlan(plan, deployAction, deployment);

        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    private void undeploy(Object deployment) throws DeploymentException {
        String runtimeName = registry.remove(deployment);
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

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction, Object deployment)
            throws Exception {
        Future<ServerDeploymentPlanResult> future = deploymentManager.execute(plan);
        registry.put(deployment, deployAction.getDeploymentUnitUniqueName());
        ServerDeploymentPlanResult planResult = future.get();

        ServerDeploymentActionResult actionResult = planResult.getDeploymentActionResult(deployAction.getId());
        if (actionResult != null) {
            Exception deploymentException = (Exception) actionResult.getDeploymentException();
            if (deploymentException != null)
                throw deploymentException;
        }
        return deployAction.getDeploymentUnitUniqueName();
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        try {
            InputStream input = archive.as(ZipExporter.class).exportAsInputStream();
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(archive.getName(), input).andDeploy();
            DeploymentPlan plan = builder.build();
            DeploymentAction deployAction = builder.getLastAction();
            executeDeploymentPlan(plan, deployAction, archive);

            return new ProtocolMetaData();
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy to container", e);
        }
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
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

    protected abstract MBeanServerConnection getMBeanServerConnection();

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

    private String executeDeploymentPlan(DeploymentPlan plan, DeploymentAction deployAction, Archive<?> archive)
            throws Exception {
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
        MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class);
        File[] resolved = resolver.artifact(filespec).resolveAsFiles(new StrictFilter());
        if (resolved == null || resolved.length == 0)
            throw new DeploymentException("Cannot obtain maven artifact: " + filespec);

        try {
            DeploymentPlanBuilder builder = deploymentManager.newDeploymentPlan();
            builder = builder.add(artifactId, resolved[0]).andDeploy();
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

    static class MBeanProxy {
        static <T> T get(MBeanServerConnection server, ObjectName name, Class<T> interf) {
            return (T) MBeanServerInvocationHandler.newProxyInstance(server, name, interf, false);
        }
    }
}