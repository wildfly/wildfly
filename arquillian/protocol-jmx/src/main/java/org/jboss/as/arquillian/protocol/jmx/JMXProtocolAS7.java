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

import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.protocol.jmx.AbstractJMXProtocol;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.shrinkwrap.api.Archive;

/**
 * The JBossAS JMXProtocol extension.
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2011
 */
public class JMXProtocolAS7 extends AbstractJMXProtocol {

    @Inject
    @SuiteScoped
    private InstanceProducer<ServiceArchiveHolder> archiveHolderInst;

    @Override
    public DeploymentPackager getPackager() {
        if(archiveHolderInst.get() == null) {
            archiveHolderInst.set(new ServiceArchiveHolder());
        }
        return new JMXProtocolPackager(archiveHolderInst.get());
    }

    @Override
    public String getProtocolName() {
        return "jmx-as7";
    }

    class ServiceArchiveHolder {
        /*
         * We store the Arquillian Service so we only create it once. It is later deployed on first Deployment that needs it.
         */
        private Archive<?> serviceArchive;

        /*
         * Hold the Archives that have been enriched with the jmx-as7 protocol so we can deploy the serviceArchive.
         * This is removed in ArquillianServiceDeployer.
         */
        private Set<String> preparedDeployments = new HashSet<String>();

        Archive<?> getArchive() {
            return serviceArchive;
        }

        void setArchive(Archive<?> serviceArchive) {
            this.serviceArchive = serviceArchive;
        }

        void addPreparedDeployment(String deploymentName) {
            if(deploymentName != null) {
                preparedDeployments.add(deploymentName);
            }
        }

        public boolean deploymentExistsAndRemove(String deploymentName) {
            if(deploymentName != null) {
                return preparedDeployments.remove(deploymentName);
            }
            return false;
        }
    }
}
