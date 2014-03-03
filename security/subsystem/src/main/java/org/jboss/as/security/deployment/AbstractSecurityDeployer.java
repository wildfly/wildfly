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

package org.jboss.as.security.deployment;

import org.jboss.as.security.service.JaccService;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * A helper class for security deployment processors
 *
 * @author Marcus Moyses
 * @author Anil Saldhana
 */
public abstract class AbstractSecurityDeployer<T> {

    public JaccService<T> deploy(DeploymentUnit deploymentUnit) {
        // build the jacc context id.
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
        return createService(jaccContextId, metaData, standalone);
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
    protected abstract JaccService<T> createService(String contextId, T metaData, Boolean standalone);

    /**
     * Return the type of metadata
     *
     * @return
     */
    protected abstract AttachmentKey<T> getMetaDataType();

}
