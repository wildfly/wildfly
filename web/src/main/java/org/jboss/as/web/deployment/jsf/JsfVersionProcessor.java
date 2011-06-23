/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web.deployment.jsf;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.JsfVersionMarker;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;

import java.util.List;

/**
 * @author Stuart Douglas
 */
public class JsfVersionProcessor implements DeploymentUnitProcessor {

    public static final String CONTEXT_PARAM = "org.jboss.jbossfaces.JSF_CONFIG_NAME";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        if (metaData == null) {
            return;
        }

        if (metaData.getMergedJBossWebMetaData() == null) {
            return;
        }

        List<ParamValueMetaData> contextParams = metaData.getMergedJBossWebMetaData().getContextParams();
        if(contextParams == null) {
            return;
        }

        //we need to set the JSF version for the whole deployment
        //as otherwise linkage errors can occur
        //if the user does have an ear with two wars with two different
        //JSF versions they are going to need to use deployment descriptors
        //to manually sort out the dependencies
        for(final ParamValueMetaData param : contextParams) {
            if(param.getParamName().equals(CONTEXT_PARAM)) {
                JsfVersionMarker.setVersion(topLevelDeployment, param.getParamValue());
                break;
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
