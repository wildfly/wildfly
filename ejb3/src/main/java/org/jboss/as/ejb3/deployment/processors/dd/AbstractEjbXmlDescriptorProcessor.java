/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;

/**
 * User: jpai
 */
public abstract class AbstractEjbXmlDescriptorProcessor<T extends EnterpriseBeanMetaData> implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // get the deployment unit
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // find the Jakarta Enterprise Beans jar metadata and start processing it
        EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }
        // process Jakarta Enterprise Beans
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
}
