/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs.deployment;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.as.weld.WeldCapability;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.modules.Module;

/**
 * @author Stuart Douglas
 */
public class JaxrsCdiIntegrationProcessor implements DeploymentUnitProcessor {

    public static final String CDI_INJECTOR_FACTORY_CLASS = "org.jboss.resteasy.cdi.CdiInjectorFactory";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        final JBossWebMetaData webdata = warMetaData.getMergedJBossWebMetaData();

        try {
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            if (support.hasCapability(WELD_CAPABILITY_NAME)) {
                final WeldCapability api = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).orElse(null);
                if (api != null && api.isWeldDeployment(deploymentUnit)) {
                    // don't set this param if Jakarta Contexts and Dependency Injection is not in classpath
                    module.getClassLoader().loadClass(CDI_INJECTOR_FACTORY_CLASS);
                    JAXRS_LOGGER.debug("Found Jakarta Contexts and Dependency Injection, adding injector factory class");
                    setContextParameter(webdata, "resteasy.injector.factory", CDI_INJECTOR_FACTORY_CLASS);
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
    }


    public static void setContextParameter(JBossWebMetaData webdata, String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        List<ParamValueMetaData> params = webdata.getContextParams();
        if (params == null) {
            params = new ArrayList<>();
            webdata.setContextParams(params);
        }
        params.add(param);
    }
}
