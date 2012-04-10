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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.deployment.JsfVersionMarker;
import org.jboss.as.web.deployment.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.WebFragmentMetaData;

/**
 * @author Stuart Douglas
 * @author Stan Silvert
 */
public class JsfVersionProcessor implements DeploymentUnitProcessor {

    public static final String JSF_CONFIG_NAME_PARAM = "org.jboss.jbossfaces.JSF_CONFIG_NAME";
    public static final String WAR_BUNDLES_JSF_IMPL_PARAM = "org.jboss.jbossfaces.WAR_BUNDLES_JSF_IMPL";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit topLevelDeployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        final WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        if (metaData == null) {
            return;
        }

        List<ParamValueMetaData> contextParams = new ArrayList<ParamValueMetaData>();

        if ((metaData.getWebMetaData() != null) && (metaData.getWebMetaData().getContextParams() != null)) {
            contextParams.addAll(metaData.getWebMetaData().getContextParams());
        }

        if (metaData.getWebFragmentsMetaData() != null) {
            for (WebFragmentMetaData fragmentMetaData : metaData.getWebFragmentsMetaData().values()) {
                if (fragmentMetaData.getContextParams() != null) {
                    contextParams.addAll(fragmentMetaData.getContextParams());
                }
            }
        }

        //we need to set the JSF version for the whole deployment
        //as otherwise linkage errors can occur
        //if the user does have an ear with two wars with two different
        //JSF versions they are going to need to use deployment descriptors
        //to manually sort out the dependencies
        for (final ParamValueMetaData param : contextParams) {
            if ((param.getParamName().equals(WAR_BUNDLES_JSF_IMPL_PARAM) &&
                    (param.getParamValue() != null) &&
                    (param.getParamValue().toLowerCase(Locale.ENGLISH).equals("true")))) {
                JsfVersionMarker.setVersion(topLevelDeployment, JsfVersionMarker.WAR_BUNDLES_JSF_IMPL);
                break; // WAR_BUNDLES_JSF_IMPL always wins
            }

            if (param.getParamName().equals(JSF_CONFIG_NAME_PARAM)) {
                JsfVersionMarker.setVersion(topLevelDeployment, param.getParamValue());
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
