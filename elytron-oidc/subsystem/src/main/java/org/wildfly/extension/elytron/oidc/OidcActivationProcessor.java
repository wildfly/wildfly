/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;
import static org.wildfly.security.http.oidc.Oidc.JSON_CONFIG_CONTEXT_PARAM;

import java.util.ArrayList;
import java.util.List;

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

/**
 * A {@link DeploymentUnitProcessor} to detect if Elytron OIDC should be activated.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class OidcActivationProcessor implements DeploymentUnitProcessor {

    private static final String OIDC_AUTH_METHOD = "OIDC";

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
        if (loginConfig != null && loginConfig.getAuthMethod().equals(OIDC_AUTH_METHOD)) {
            ListenerMetaData listenerMetaData = new ListenerMetaData();
            listenerMetaData.setListenerClass(OidcConfigurationServletListener.class.getName());
            webMetaData.getListeners().add(listenerMetaData);

            ROOT_LOGGER.tracef("Activating OIDC for deployment %s.", deploymentUnit.getName());
            OidcDeploymentMarker.mark(deploymentUnit);
            VirtualHttpServerMechanismFactoryMarkerUtility.virtualMechanismFactoryRequired(deploymentUnit);
        }
    }

    private void addOidcAuthDataAndConfig(DeploymentPhaseContext phaseContext, OidcConfigService service, JBossWebMetaData webMetaData) throws DeploymentUnitProcessingException {
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

    @Override
    public void undeploy(DeploymentUnit context) {}

}