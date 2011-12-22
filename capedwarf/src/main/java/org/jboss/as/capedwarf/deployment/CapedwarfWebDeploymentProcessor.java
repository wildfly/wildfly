/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

/**
 * CapeDwarf modifying web content processor.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public abstract class CapedwarfWebDeploymentProcessor implements DeploymentUnitProcessor {

    protected Logger log = Logger.getLogger(getClass());

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        if (CapedwarfDeploymentMarker.isCapedwarfDeployment(unit) == false)
            return;

        doDeploy(unit);

        WebMetaData webMetaData = getWebMetaData(unit);
        if (webMetaData != null)
            doDeploy(unit, webMetaData);

        JBossWebMetaData jBossWebMetaData = getJBossWebMetaData(unit);
        if (webMetaData != null)
            doDeploy(unit, jBossWebMetaData);
    }

    protected WebMetaData getWebMetaData(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        return (warMetaData != null ? warMetaData.getWebMetaData() : null);
    }

    protected JBossWebMetaData getJBossWebMetaData(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        return (warMetaData != null ? warMetaData.getMergedJBossWebMetaData() : null);
    }

    protected void doDeploy(DeploymentUnit unit) {
    }

    protected void doDeploy(DeploymentUnit unit, WebMetaData webMetaData) {
    }

    protected void doDeploy(DeploymentUnit unit, JBossWebMetaData webMetaData) {
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
