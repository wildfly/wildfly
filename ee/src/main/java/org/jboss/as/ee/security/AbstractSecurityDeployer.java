/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.security;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;

/**
 * A helper class for security deployment processors
 *
 * @author Marcus Moyses
 * @author Anil Saldhana
 */
public abstract class AbstractSecurityDeployer<T> {

    public JaccService<T> deploy(DeploymentUnit deploymentUnit) {
        // build the Jakarta Authorization context id.
        String contextId = deploymentUnit.getName();
        if (deploymentUnit.getParent() != null) {
            contextId = deploymentUnit.getParent().getName() + "!" + contextId;
        }
        return deploy(deploymentUnit, contextId);
    }

    public JaccService<T> deploy(DeploymentUnit deploymentUnit, String jaccContextId) {
        T metaData = deploymentUnit.getAttachment(getMetaDataType());
        Boolean standalone = Boolean.FALSE;
        // check if it is top level
        if (deploymentUnit.getParent() == null) {
            standalone = Boolean.TRUE;
        }
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        return createService(jaccContextId, metaData, standalone, module.getClassLoader());
    }

    public void undeploy(DeploymentUnit deploymentUnit) {

    }

    /**
     * Creates the appropriate service for metaData T
     * @param contextId
     * @param metaData
     * @param standalone
     * @return
     */
    protected abstract JaccService<T> createService(String contextId, T metaData, Boolean standalone, ClassLoader deploymentClassLoader);

    /**
     * Return the type of metadata
     *
     * @return
     */
    protected abstract AttachmentKey<T> getMetaDataType();

}
