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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.as.arquillian.protocol.jmx.JMXProtocolAS7.ServiceArchiveHolder;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;

/**
 * A deployer for the Arquillian JMXProtocol endpoint.
 *
 * @see JMXProtocolPackager
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2011
 */
public class ArquillianServiceDeployer {

    private static final Logger log = Logger.getLogger(ArquillianServiceDeployer.class);

    @Inject
    @SuiteScoped
    private Instance<ServiceArchiveHolder> archiveHolderInst;

    @Inject
    @ContainerScoped
    private Instance<Container> containerInst;

    private AtomicBoolean serviceArchiveDeployed = new AtomicBoolean();

    public synchronized void doServiceDeploy(@Observes BeforeDeploy event) {
        ServiceArchiveHolder archiveHolder = archiveHolderInst.get();
        if (archiveHolder != null && serviceArchiveDeployed.get() == false) {
            try {
                Archive<?> archive = archiveHolder.getArchive();
                log.infof("Deploy arquillian service: %s", archive);
                DeployableContainer<?> deployableContainer = containerInst.get().getDeployableContainer();
                deployableContainer.deploy(archive);
                serviceArchiveDeployed.set(true);
            } catch (Throwable th) {
                log.error("Cannot deploy arquillian service", th);
            }
        }
    }

    public synchronized void undeploy(@Observes BeforeStop event) {
        ServiceArchiveHolder archiveHolder = archiveHolderInst.get();
        if (archiveHolder != null && serviceArchiveDeployed.get() == true) {
            try {
                Archive<?> archive = archiveHolder.getArchive();
                log.infof("Undeploy arquillian service: %s", archive);
                DeployableContainer<?> deployableContainer = containerInst.get().getDeployableContainer();
                deployableContainer.undeploy(archive);
                serviceArchiveDeployed.set(false);
            } catch (Throwable th) {
                log.error("Cannot undeploy arquillian service", th);
            }
        }
    }
}
