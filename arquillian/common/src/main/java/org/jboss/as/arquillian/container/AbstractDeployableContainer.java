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

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.modules.management.ObjectProperties;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.util.NotImplementedException;

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

    private T containerConfig;
    private ModelControllerClient modelControllerClient;

    @Inject
    @ContainerScoped
    private InstanceProducer<ArchiveDeployer> archiveDeployerInst;

    private final Map<Object, String> registry = new HashMap<Object, String>();

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("jmx-as7");
    }

    @Override
    public void setup(T config) {
        containerConfig = config;
        modelControllerClient = ModelControllerClient.Factory.create(config.getBindAddress(), config.getManagementPort());
        ArchiveDeployer archiveDeployer = new ArchiveDeployer(ServerDeploymentManager.Factory.create(modelControllerClient));
        archiveDeployerInst.set(archiveDeployer);
    }

    @Override
    public final void start() throws LifecycleException {
        startInternal();
    }

    protected abstract void startInternal() throws LifecycleException;

    @Override
    public final void stop() throws LifecycleException {
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
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        ArchiveDeployer archiveDeployer = archiveDeployerInst.get();
        String runtimeName = archiveDeployer.deploy(archive);
        registry.put(archive, runtimeName);
        return new ProtocolMetaData();
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        String runtimeName = registry.remove(archive);
        if (runtimeName != null) {
            ArchiveDeployer archiveDeployer = archiveDeployerInst.get();
            archiveDeployer.undeploy(runtimeName);
        }
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new NotImplementedException();
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new NotImplementedException();
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

    static class MBeanProxy {
        static <T> T get(MBeanServerConnection server, ObjectName name, Class<T> interf) {
            return (T) MBeanServerInvocationHandler.newProxyInstance(server, name, interf, false);
        }
    }
}