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
package org.jboss.as.arquillian.container.remote;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.as.arquillian.container.CommonDeployableContainer;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;

/**
 * JBossASRemoteContainer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class RemoteDeployableContainer extends
        CommonDeployableContainer<RemoteContainerConfiguration> {

    private MBeanServerConnectionProvider provider;

    @Inject
    @ContainerScoped
    private InstanceProducer<MBeanServerConnection> mbeanServerInst;

    @Override
    public void setup(RemoteContainerConfiguration config) {
        super.setup(config);
        provider = new MBeanServerConnectionProvider(config.getBindAddress(),
                config.getJmxPort());
    }

    @Override
    protected void startInternal() throws LifecycleException {
        mbeanServerInst.set(getMBeanServerConnection());
    }

    @Override
    protected void stopInternal() throws LifecycleException {
    }

    @Override
    public Class<RemoteContainerConfiguration> getConfigurationClass() {
        return RemoteContainerConfiguration.class;
    }

    @Override
    protected MBeanServerConnection getMBeanServerConnection() {
        return provider.getConnection();
    }
}