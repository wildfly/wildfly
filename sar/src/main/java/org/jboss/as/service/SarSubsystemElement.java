/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.service;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProcessorInjector;
import org.jboss.as.deployment.chain.JarDeploymentActivator;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitProcessorService;
import org.jboss.as.model.AbstractSubsystemElement;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.staxmapper.XMLExtendedStreamWriter;


/**
 * SAR deployment subsystem element.
 *
 * @author John Bailey
 */
final class SarSubsystemElement extends AbstractSubsystemElement<SarSubsystemElement> {

    private static final long serialVersionUID = -5742210831771948955L;

    public static final long SAR_DEPLOYMENT_CHAIN_PRIORITY = JarDeploymentActivator.JAR_DEPLOYMENT_CHAIN_PRIORITY - 1000000L;
    public static final ServiceName SAR_DEPLOYMENT_CHAIN_SERVICE_NAME = DeploymentChain.SERVICE_NAME.append("sar");

    SarSubsystemElement(){
        super(SarExtension.NAMESPACE);
    }

    private <T extends DeploymentUnitProcessor> BatchServiceBuilder<T> addDeploymentProcessor(final BatchBuilder batchBuilder, final T deploymentUnitProcessor, final long priority) {
        final DeploymentUnitProcessorService<T> deploymentUnitProcessorService = new DeploymentUnitProcessorService<T>(deploymentUnitProcessor);
        return batchBuilder.addService(SAR_DEPLOYMENT_CHAIN_SERVICE_NAME.append(deploymentUnitProcessor.getClass().getName()), deploymentUnitProcessorService)
            .addDependency(SAR_DEPLOYMENT_CHAIN_SERVICE_NAME, DeploymentChain.class, new DeploymentChainProcessorInjector<T>(deploymentUnitProcessorService, priority));
    }

    /** {@inheritDoc} */
    protected Class<SarSubsystemElement> getElementClass() {
        return SarSubsystemElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeEndElement();
    }

    /** {@inheritDoc} */
    protected void getClearingUpdates(List<? super AbstractSubsystemUpdate<SarSubsystemElement, ?>> list) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    protected boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }
}
