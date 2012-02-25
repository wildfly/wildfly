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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.arquillian.protocol.jmx.JMXProtocolAS7.ServiceArchiveHolder;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

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

    private Set<String> serviceArchiveDeployed = new HashSet<String>();

    public synchronized void doServiceDeploy(@Observes BeforeDeploy event, Container container, ServiceArchiveHolder archiveHolder) {
        // already deployed?
        if(serviceArchiveDeployed.contains(container.getName())) {
           archiveHolder.deploymentExistsAndRemove(event.getDeployment().getName()); // cleanup
           return;
        }

        // only deploy the service if the deployment has been enriched by the jmx-as7 protocol
        if(archiveHolder.deploymentExistsAndRemove(event.getDeployment().getName())) {
            JavaArchive serviceArchive = (JavaArchive) archiveHolder.getArchive();
            try {
                log.infof("Deploy arquillian service: %s", serviceArchive);
                final Map<String, String> props = container.getContainerConfiguration().getContainerProperties();
                    //MASSIVE HACK
                    //write the management connection props to the archive, so we can access them from the server
                    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bytes);
                    out.writeObject(props.get("managementPort"));
                    out.writeObject(props.get("managementAddress"));
                    out.close();
                    serviceArchive.addAsManifestResource(new ByteArrayAsset(bytes.toByteArray()), "org.jboss.as.managementConnectionProps");

                DeployableContainer<?> deployableContainer = container.getDeployableContainer();
                deployableContainer.deploy(serviceArchive);
                serviceArchiveDeployed.add(container.getName());
            } catch (Throwable th) {
                log.error("Cannot deploy arquillian service", th);
            }
        }
    }

    public synchronized void undeploy(@Observes BeforeStop event, Container container, ServiceArchiveHolder archiveHolder) {
        // clean up if we deployed to this container?
        if(serviceArchiveDeployed.contains(container.getName())) {
            try {
                Archive<?> serviceArchive = archiveHolder.getArchive();
                log.infof("Undeploy arquillian service: %s", serviceArchive);
                DeployableContainer<?> deployableContainer = container.getDeployableContainer();
                deployableContainer.undeploy(serviceArchive);
                serviceArchiveDeployed.remove(container.getName());
            } catch (Throwable th) {
                log.error("Cannot undeploy arquillian service", th);
            }
        }
    }
}
