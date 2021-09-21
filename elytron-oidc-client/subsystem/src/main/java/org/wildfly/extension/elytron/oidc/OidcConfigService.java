/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

public final class OidcConfigService {

    private static final String CREDENTIALS_JSON_NAME = "credentials";

    private static final String REDIRECT_REWRITE_RULE_JSON_NAME = "redirect-rewrite-rules";

    private static final OidcConfigService INSTANCE = new OidcConfigService();

    public static OidcConfigService getInstance() {
        return INSTANCE;
    }

    private final Map<String, ModelNode> realms = new HashMap<String, ModelNode>();
    private final Map<String, ModelNode> providers = new HashMap<String, ModelNode>();

    // deployments secured with OpenID Connect
    private final Map<String, ModelNode> secureDeployments = new HashMap<String, ModelNode>();

    private OidcConfigService() {
    }

    public void addRealm(String realmName, ModelNode model) {
        this.realms.put(realmName, model.clone());
    }

    public void updateRealm(String realmName, String attrName, ModelNode resolvedValue) {
        ModelNode realm = this.realms.get(realmName);
        realm.get(attrName).set(resolvedValue);
    }

    public void removeRealm(String realmName) {
        this.realms.remove(realmName);
    }

    public void addProvider(String providerName, ModelNode model) {
        this.providers.put(providerName, model.clone());
    }

    public void updateProvider(String providerName, String attrName, ModelNode resolvedValue) {
        ModelNode realm = this.providers.get(providerName);
        realm.get(attrName).set(resolvedValue);
    }

    public void removeProvider(String providerName) {
        this.providers.remove(providerName);
    }

    public void addSecureDeployment(String deploymentName, ModelNode model) {
        ModelNode deployment = model.clone();
        this.secureDeployments.put(deploymentName, deployment);
    }

    public void updateSecureDeployment(String deploymentName, String attrName, ModelNode resolvedValue) {
        ModelNode deployment = this.secureDeployments.get(deploymentName);
        deployment.get(attrName).set(resolvedValue);
    }

    public void removeSecureDeployment(String deploymentName) {
        this.secureDeployments.remove(deploymentName);
    }

    public void addCredential(String deploymentName, String credentialName, ModelNode model) {
        ModelNode credentials = getCredentialsForDeployment(deploymentName);
        if (! credentials.isDefined()) {
            credentials = new ModelNode();
        }

        setCredential(credentialName, model, credentials);
        ModelNode deployment = this.secureDeployments.get(deploymentName);
        deployment.get(CREDENTIALS_JSON_NAME).set(credentials);
    }

    private void setCredential(String credentialName, ModelNode model, ModelNode credentials) {
        if (credentialName.equals(ElytronOidcDescriptionConstants.SECRET)) {
            credentials.get(credentialName).set(model.get(ElytronOidcDescriptionConstants.SECRET).asString());
        } else {
            credentials.set(credentialName, getCredentialValue(model));
        }
    }

    private ModelNode getCredentialValue(ModelNode model) {
        ModelNode credential = new ModelNode();
        for (SimpleAttributeDefinition attribute : CredentialDefinition.ATTRIBUTES) {
            String attributeName = attribute.getName();
            ModelNode attributeValue = model.get(attributeName);
            if (attributeValue.isDefined()) {
                credential.get(attributeName).set(attributeValue.asString());
            }
        }
        return credential;
    }

    public void removeCredential(String deploymentName, String credentialName) {
        ModelNode credentials = getCredentialsForDeployment(deploymentName);
        if (!credentials.isDefined()) {
            throw ROOT_LOGGER.cannotRemoveCredential(deploymentName);
        }
        credentials.remove(credentialName);
    }

    public void updateCredential(String deploymentName, String credentialName, ModelNode resolvedValue) {
        ModelNode credentials = getCredentialsForDeployment(deploymentName);
        if (!credentials.isDefined()) {
            throw ROOT_LOGGER.cannotUpdateCredential(deploymentName);
        }
        setCredential(credentialName, resolvedValue, credentials);
    }

    private ModelNode getCredentialsForDeployment(String deploymentName) {
        ModelNode deployment = this.secureDeployments.get(deploymentName);
        return deployment.get(CREDENTIALS_JSON_NAME);
    }

    public void addRedirectRewriteRule(String deploymentName, String redirectRewriteRuleName, ModelNode model) {
        ModelNode redirectRewritesRules = getRedirectRewriteRuleForDeployment(deploymentName);
        if (!redirectRewritesRules.isDefined()) {
            redirectRewritesRules = new ModelNode();
        }
        redirectRewritesRules.get(redirectRewriteRuleName).set(model.get(ElytronOidcDescriptionConstants.REPLACEMENT).asString());

        ModelNode deployment = this.secureDeployments.get(deploymentName);
        deployment.get(REDIRECT_REWRITE_RULE_JSON_NAME).set(redirectRewritesRules);
    }

    public void removeRedirectRewriteRule(String deploymentName, String redirectRewriteRuleName) {
        ModelNode redirectRewritesRules = getRedirectRewriteRuleForDeployment(deploymentName);
        if (!redirectRewritesRules.isDefined()) {
            throw ROOT_LOGGER.cannotRemoveRedirectRuntimeRule(deploymentName);
        }
        redirectRewritesRules.remove(redirectRewriteRuleName);
    }

    public void updateRedirectRewriteRule(String deploymentName, String redirectRewriteRuleName, ModelNode resolvedValue) {
        ModelNode redirectRewritesRules = getRedirectRewriteRuleForDeployment(deploymentName);
        if (!redirectRewritesRules.isDefined()) {
            throw ROOT_LOGGER.cannotUpdateRedirectRuntimeRule(deploymentName);
        }
        redirectRewritesRules.get(redirectRewriteRuleName).set(resolvedValue.get(ElytronOidcDescriptionConstants.REPLACEMENT).asString());
    }

    private ModelNode getRedirectRewriteRuleForDeployment(String deploymentName) {
        ModelNode deployment = this.secureDeployments.get(deploymentName);
        return deployment.get(REDIRECT_REWRITE_RULE_JSON_NAME);
    }

    protected boolean isDeploymentConfigured(DeploymentUnit deploymentUnit) {
        ModelNode deployment = getSecureDeployment(deploymentUnit);
        if (! deployment.isDefined()) {
            return false;
        }
        ModelNode resource = deployment.get(SecureDeploymentDefinition.RESOURCE.getName());
        ModelNode clientId = deployment.get(SecureDeploymentDefinition.CLIENT_ID.getName());
        return resource.isDefined() || clientId.isDefined();
    }

    public String getJSON(DeploymentUnit deploymentUnit) {
        ModelNode deployment = getSecureDeployment(deploymentUnit);
        return getJSON(deployment);
    }

    public String getJSON(String deploymentName) {
        ModelNode deployment = this.secureDeployments.get(deploymentName);
        return getJSON(deployment);
    }

    private String getJSON(ModelNode deployment) {
        ModelNode json = new ModelNode();
        ModelNode realmOrProvider = null;
        String realmName = deployment.get(ElytronOidcDescriptionConstants.REALM).asStringOrNull();
        if (realmName != null) {
            realmOrProvider = this.realms.get(realmName);
            json.get(ElytronOidcDescriptionConstants.REALM).set(realmName);
        } else {
            String providerName = deployment.get(ElytronOidcDescriptionConstants.PROVIDER).asStringOrNull();
            if (providerName != null) {
                realmOrProvider = this.providers.get(providerName);
                deployment.remove(ElytronOidcDescriptionConstants.PROVIDER);
            }
        }

        // set realm/provider values first, some can be overridden by deployment values
        if (realmOrProvider != null) {
            setJSONValues(json, realmOrProvider);
        }
        setJSONValues(json, deployment);
        return json.toJSONString(true);
    }

    private void setJSONValues(ModelNode json, ModelNode values) {
        synchronized (values) {
            for (Property prop : new ArrayList<>(values.asPropertyList())) {
                String name = prop.getName();
                ModelNode value = prop.getValue();
                if (value.isDefined()) {
                    json.get(name).set(value);
                }
            }
        }
    }

    public boolean isSecureDeployment(DeploymentUnit deploymentUnit) {
        String deploymentName = preferredDeploymentName(deploymentUnit);
        return this.secureDeployments.containsKey(deploymentName);
    }

    private ModelNode getSecureDeployment(DeploymentUnit deploymentUnit) {
        String deploymentName = preferredDeploymentName(deploymentUnit);
        return this.secureDeployments.containsKey(deploymentName)
                ? this.secureDeployments.get(deploymentName)
                : new ModelNode();
    }

    private String preferredDeploymentName(DeploymentUnit deploymentUnit) {
        String deploymentName = deploymentUnit.getName();
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return deploymentName;
        }

        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            return deploymentName;
        }

        String moduleName = webMetaData.getModuleName();
        if (moduleName != null) return moduleName + ".war";

        return deploymentName;
    }

}
