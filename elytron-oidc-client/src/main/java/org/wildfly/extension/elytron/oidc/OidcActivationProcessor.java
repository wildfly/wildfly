/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;
import static org.wildfly.security.http.oidc.Oidc.JSON_CONFIG_CONTEXT_PARAM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.VirtualHttpServerMechanismFactoryMarkerUtility;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.wildfly.security.http.oidc.OidcConfigurationServletListener;

import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.AUTHENTICATION_REQUEST_FORMAT;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_ENCRYPTION_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_ALIAS;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEY_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE;

/**
 * A {@link DeploymentUnitProcessor} to detect if Elytron OIDC should be activated.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class OidcActivationProcessor implements DeploymentUnitProcessor {

    private static final String OIDC_AUTH_METHOD = "OIDC";
    private static final String JSON_CONFIG_UNSUPPORTED_ATTRIBUTE_PARAM = "org.wildfly.security.http.oidc.json.config.unsupported.attributes";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        OidcConfigService configService = OidcConfigService.getInstance();
        if (configService.isSecureDeployment(deploymentUnit) && configService.isDeploymentConfigured(deploymentUnit)) {
            addOidcAuthDataAndConfig(phaseContext, configService, webMetaData);
        }

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig != null && OIDC_AUTH_METHOD.equals(loginConfig.getAuthMethod())) {
            ListenerMetaData listenerMetaData = new ListenerMetaData();
            listenerMetaData.setListenerClass(OidcConfigurationServletListener.class.getName());
            webMetaData.getListeners().add(listenerMetaData);

            ROOT_LOGGER.tracef("Activating OIDC for deployment %s.", deploymentUnit.getName());
            OidcDeploymentMarker.mark(deploymentUnit);
            VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryRequired(deploymentUnit);
        }
        Map<String, SimpleAttributeDefinition> unsupportedAttributes = new HashMap<>();
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT, AUTHENTICATION_REQUEST_FORMAT);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ALGORITHM, REQUEST_OBJECT_ENCRYPTION_ALGORITHM);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_ALGORITHM, REQUEST_OBJECT_SIGNING_ALGORITHM);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_ALIAS, REQUEST_OBJECT_SIGNING_KEY_ALIAS);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEY_PASSWORD, REQUEST_OBJECT_SIGNING_KEY_PASSWORD);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_FILE, REQUEST_OBJECT_SIGNING_KEYSTORE_FILE);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD, REQUEST_OBJECT_SIGNING_KEYSTORE_PASSWORD);
        unsupportedAttributes.put(ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE, REQUEST_OBJECT_SIGNING_KEYSTORE_TYPE);
        addUnsupportedAttributesToWebMetaData(deploymentUnit, webMetaData, unsupportedAttributes);
    }

    private void addOidcAuthDataAndConfig(DeploymentPhaseContext phaseContext, OidcConfigService service, JBossWebMetaData webMetaData) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        addJSONDataAsContextParam(service.getJSON(deploymentUnit), webMetaData);

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig == null) {
            loginConfig = new LoginConfigMetaData();
            webMetaData.setLoginConfig(loginConfig);
        }
        loginConfig.setAuthMethod(OIDC_AUTH_METHOD);
        ROOT_LOGGER.deploymentSecured(deploymentUnit.getName());
    }

    private void addJSONDataAsContextParam(String json, JBossWebMetaData webMetaData) {
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<>();
        }

        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(JSON_CONFIG_CONTEXT_PARAM);
        param.setParamValue(json);
        contextParams.add(param);
        webMetaData.setContextParams(contextParams);
    }

    private static void addUnsupportedAttributesToWebMetaData(DeploymentUnit deploymentUnit, JBossWebMetaData webMetaData, Map<String, SimpleAttributeDefinition> unsupportedAttributes) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(JSON_CONFIG_UNSUPPORTED_ATTRIBUTE_PARAM);
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();

        for (Map.Entry<String, SimpleAttributeDefinition> entry : unsupportedAttributes.entrySet()) {
            if (!deploymentUnit.enables(entry.getValue())) {
                if (param.getParamValue() == null) {
                    param.setParamValue(entry.getKey());
                } else {
                    param.setParamValue(param.getParamValue() + " " + entry.getKey());
                }
            }
        }

        if (contextParams == null) {
            contextParams = new ArrayList<>();
        }
        contextParams.add(param);
        webMetaData.setContextParams(contextParams);
    }
}