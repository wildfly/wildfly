/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.ModuleIdentifierUtil;
import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.MultipartConfigMetaData;

import com.sun.faces.config.WebConfiguration;

/**
 * @author Stuart Douglas
 */
public class JSFMetadataProcessor implements DeploymentUnitProcessor {

    public static final String JAVAX_FACES_WEBAPP_FACES_SERVLET = "jakarta.faces.webapp.FacesServlet";

    private final Boolean disallowDoctypeDecl;

    public JSFMetadataProcessor(final Boolean disallowDoctypeDecl) {
        this.disallowDoctypeDecl = disallowDoctypeDecl;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData metaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(JsfVersionMarker.isJsfDisabled(deploymentUnit)) {
            return;
        }
        JBossWebMetaData webMetaData = (metaData != null) ? metaData.getMergedJBossWebMetaData() : null;
        if (webMetaData == null || webMetaData.getServlets() == null) {
            return;
        }
        JBossServletMetaData jsf = null;
        for(JBossServletMetaData servlet : webMetaData.getServlets()) {
            if(JAVAX_FACES_WEBAPP_FACES_SERVLET.equals(servlet.getServletClass())) {
                jsf = servlet;
            }
        }
        if (jsf != null && jsf.getMultipartConfig() == null) {
            // WFLY-2329 File upload doesn't work
            jsf.setMultipartConfig(new MultipartConfigMetaData());
        }
        if (disallowDoctypeDecl != null) {
            // Add the disallowDoctypeDecl context param if it's not already present
            setContextParameterIfAbsent(webMetaData, WebConfiguration.BooleanWebContextInitParameter.DisallowDoctypeDecl.getQualifiedName(), disallowDoctypeDecl.toString());
        }
        if (webMetaData.getDistributable() != null) {
            // Auto-disable lazy bean validation for distributable web application.
            // This can otherwise cause missing @PreDestroy events.
            String disabled = Boolean.toString(false);
            if (!setContextParameterIfAbsent(webMetaData, WebConfiguration.BooleanWebContextInitParameter.EnableLazyBeanValidation.getQualifiedName(), disabled).equals(disabled)) {
                JSFLogger.ROOT_LOGGER.lazyBeanValidationEnabled();
            }

            String version = JsfVersionMarker.getVersion(deploymentUnit);
            // Disable counter-productive "distributable" logic in Mojarra implementation
            if (version.equals(JsfVersionMarker.JSF_4_0)) {
                // Retrieve the canonical module identifier string
                String implModId = JSFModuleIdFactory.getInstance().getImplModId(version);

                // Now compare the canonical module identifier (which doesn't include "main" as the slot)
                String canonicalModId = ModuleIdentifierUtil.canonicalModuleIdentifier(implModId);
                // Check if the canonical module identifier is for the "main" slot
                if (canonicalModId != null && !canonicalModId.contains(":")) {
                    setContextParameterIfAbsent(
                            webMetaData,
                            WebConfiguration.BooleanWebContextInitParameter.EnableDistributable.getQualifiedName(),
                            Boolean.FALSE.toString()
                    );
                }
            }
        }
    }

    private static String setContextParameterIfAbsent(final JBossWebMetaData webMetaData, final String name, final String value) {
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<>();
            webMetaData.setContextParams(contextParams);
        }
        for (ParamValueMetaData param : contextParams) {
            if (name.equals(param.getParamName()) && param.getParamValue() != null) {
                // already set
                return param.getParamValue();
            }
        }
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        contextParams.add(param);
        return value;
    }
}
