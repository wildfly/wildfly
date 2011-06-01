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

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.as.arquillian.container.ArchiveDeployer;
import org.jboss.as.arquillian.protocol.jmx.JBossASProtocol.ServiceArchiveHolder;
import org.jboss.logging.Logger;

/**
 * ArquillianServiceDeployer
 *
 * @author thomas.diesler@jboss.com
 * @since 31-May-2011
 */
public class ArquillianServiceDeployer {

    private static final Logger log = Logger.getLogger(ManifestBuilder.class);

    @Inject
    @SuiteScoped
    private Instance<ServiceArchiveHolder> archiveHolderInst;

    @Inject
    @ContainerScoped
    private Instance<ArchiveDeployer> archiveDeployerInst;

    private String runtimeName;

    public void doServiceDeploy(@Observes BeforeDeploy event) {
        if (runtimeName == null) {
            try {
                ServiceArchiveHolder archiveHolder = archiveHolderInst.get();
                ArchiveDeployer archiveDeployer = archiveDeployerInst.get();
                runtimeName = archiveDeployer.deploy(archiveHolder.getArchive());
            } catch (DeploymentException ex) {
                log.error(ex);
            }
        }
    }

    public void undeploy(@Observes BeforeStop event) {
        if (runtimeName != null) {
            try {
                ArchiveDeployer archiveDeployer = archiveDeployerInst.get();
                archiveDeployer.undeploy(runtimeName);
            } catch (DeploymentException ex) {
                log.error(ex);
            }
        }
    }
}
