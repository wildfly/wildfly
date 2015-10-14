/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;

/**
 * User: jpai
 */
public abstract class AbstractEjbXmlDescriptorProcessor<T extends EnterpriseBeanMetaData> implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // get the deployment unit
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // find the EJB jar metadata and start processing it
        EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }
        // process EJBs
        EnterpriseBeansMetaData ejbs = ejbJarMetaData.getEnterpriseBeans();
        if (ejbs != null && !ejbs.isEmpty()) {
            for (EnterpriseBeanMetaData ejb : ejbs) {
                if (this.getMetaDataType().isInstance(ejb)) {
                    this.processBeanMetaData((T) ejb, phaseContext);
                }
            }
        }
    }

    protected abstract Class<T> getMetaDataType();

    protected abstract void processBeanMetaData(T beanMetaData, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException;

    protected MethodIntf getMethodIntf(MethodInterfaceType viewType) {
        if (viewType == null) {
            return MethodIntf.BEAN;
        }
        switch (viewType) {
            case Home:
                return MethodIntf.HOME;
            case LocalHome:
                return MethodIntf.LOCAL_HOME;
            case ServiceEndpoint:
                return MethodIntf.SERVICE_ENDPOINT;
            case Local:
                return MethodIntf.LOCAL;
            case Remote:
                return MethodIntf.REMOTE;
            // TODO: Need to handle more recent ones (like timer, mdb)
        }
        return MethodIntf.BEAN;
    }

    protected String[] getMethodParams(MethodParametersMetaData methodParametersMetaData) {
        if (methodParametersMetaData == null) {
            return null;
        }
        return methodParametersMetaData.toArray(new String[0]);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
