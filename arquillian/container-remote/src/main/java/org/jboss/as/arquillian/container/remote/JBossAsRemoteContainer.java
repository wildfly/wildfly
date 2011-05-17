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

import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor;
import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor.ExecutionType;
import org.jboss.arquillian.spi.Configuration;
import org.jboss.arquillian.spi.ContainerMethodExecutor;
import org.jboss.arquillian.spi.Context;
import org.jboss.arquillian.spi.LifecycleException;
import org.jboss.as.arquillian.container.AbstractDeployableContainer;
import org.jboss.as.arquillian.container.JBossAsContainerConfiguration;
import org.jboss.as.arquillian.container.MBeanServerConnectionProvider;

/**
 * JBossASRemoteContainer
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public final class JBossAsRemoteContainer extends AbstractDeployableContainer {

    private MBeanServerConnectionProvider provider;

    @Override
    public void setup(Context context, Configuration configuration) {
        super.setup(context, configuration);
        JBossAsContainerConfiguration config = getContainerConfiguration();
        provider = new MBeanServerConnectionProvider(config.getBindAddress(), config.getJmxPort());
    }

    @Override
    protected void startInternal(Context context) throws LifecycleException {
        // nothing to do
    }

    @Override
    protected void stopInternal(Context context) throws LifecycleException {
        // nothing to do
    }

    @Override
    protected MBeanServerConnection getMBeanServerConnection() {
        return provider.getConnection();
    }

    @Override
    protected ContainerMethodExecutor getContainerMethodExecutor() {
        return new JMXMethodExecutor(getMBeanServerConnection(), ExecutionType.REMOTE);
    }
}