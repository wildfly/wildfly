/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;
import static org.wildfly.security.http.oidc.Oidc.JSON_CONFIG_CONTEXT_PARAM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.server.deployment.Attachments;
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
import org.jboss.vfs.VirtualFile;
import org.wildfly.security.http.oidc.OidcConfigurationServletListener;

/**
 * A {@link DeploymentUnitProcessor} to detect if Elytron OIDC should be activated.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class OidcActivationProcessor implements DeploymentUnitProcessor {

    public static final String OIDC_AUTH_METHOD = "OIDC";

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
        boolean subsystemConfigured = configService.isSecureDeployment(deploymentUnit) && configService.isDeploymentConfigured(deploymentUnit);
        if (subsystemConfigured) {
            addOidcAuthDataAndConfig(phaseContext, configService, webMetaData);
        }

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();
        if (loginConfig != null && OIDC_AUTH_METHOD.equals(loginConfig.getAuthMethod())) {
            if (! subsystemConfigured) {
                // check for unsupported attributes in the deployment's oidc.json file
                if (deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT) != null) {
                    VirtualFile oidcConfigurationFile = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot().getChild("WEB-INF/oidc.json");
                    if (oidcConfigurationFile.exists()) {
                        try (InputStream is = oidcConfigurationFile.openStream()) {
                            String oidcConfigString = readFromInputStream(is);

                            for (SimpleAttributeDefinition attribute : SecureDeploymentDefinition.NON_DEFAULT_ATTRIBUTES) {
                                if ((!deploymentUnit.enables(attribute)) && oidcConfigString.contains(attribute.getName())) {
                                    throw ROOT_LOGGER.unsupportedAttribute(attribute.getName());
                                }
                            }
                            addJSONDataAsContextParam(oidcConfigString, webMetaData);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            ListenerMetaData listenerMetaData = new ListenerMetaData();
            listenerMetaData.setListenerClass(OidcConfigurationServletListener.class.getName());
            webMetaData.getListeners().add(listenerMetaData);

            ROOT_LOGGER.tracef("Activating OIDC for deployment %s.", deploymentUnit.getName());
            OidcDeploymentMarker.mark(deploymentUnit);
            VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryRequired(deploymentUnit);
        }
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

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder oidcConfigFileStringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                oidcConfigFileStringBuilder.append(line).append("\n");
            }
        }
        return oidcConfigFileStringBuilder.toString();
    }
}