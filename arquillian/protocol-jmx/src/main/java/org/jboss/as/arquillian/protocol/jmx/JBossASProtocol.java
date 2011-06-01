/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
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
package org.jboss.as.arquillian.protocol.jmx;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.protocol.jmx.JMXMethodExecutor;
import org.jboss.arquillian.protocol.jmx.JMXProtocol;
import org.jboss.arquillian.protocol.jmx.JMXProtocolConfiguration;
import org.jboss.arquillian.protocol.jmx.JMXProtocolConfiguration.ExecutionType;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.shrinkwrap.api.Archive;

/**
 * JBossASProtocol
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2011
 */
public class JBossASProtocol extends JMXProtocol {

    @Inject
    @ContainerScoped
    private Instance<MBeanServerConnection> mbeanServerInst;

    @Inject
    @SuiteScoped
    private InstanceProducer<ServiceArchiveHolder> archiveHolderInst;

    @Override
    public DeploymentPackager getPackager() {
        archiveHolderInst.set(new ServiceArchiveHolder());
        return new JBossASDeploymentPackager(archiveHolderInst.get());
    }

    @Override
    public String getProtocolName() {
        return "jmx-as7";
    }

    @Override
    // [ARQ-425] config parser code not in sync with schema
    // Remove explicit execution type
    public ContainerMethodExecutor getExecutor(JMXProtocolConfiguration config, ProtocolMetaData metaData, CommandCallback callback) {
        MBeanServerConnection mbeanServer = mbeanServerInst.get();
        return new JMXMethodExecutor(mbeanServer, ExecutionType.REMOTE, callback);
    }

    class ServiceArchiveHolder {
        private Archive<?> serviceArchive;

        Archive<?> getArchive() {
            return serviceArchive;
        }

        void setArchive(Archive<?> serviceArchive) {
            this.serviceArchive = serviceArchive;
        }
    }
}
