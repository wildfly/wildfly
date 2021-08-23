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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
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

    public void addRealm(ModelNode operation, ModelNode model) {
        this.realms.put(realmNameFromOp(operation), model.clone());
    }

    public void updateRealm(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode realm = this.realms.get(realmNameFromOp(operation));
        realm.get(attrName).set(resolvedValue);
    }

    public void removeRealm(ModelNode operation) {
        this.realms.remove(realmNameFromOp(operation));
    }

    public void addProvider(ModelNode operation, ModelNode model) {
        this.providers.put(providerNameFromOp(operation), model.clone());
    }

    public void updateProvider(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode realm = this.providers.get(providerNameFromOp(operation));
        realm.get(attrName).set(resolvedValue);
    }

    public void removeProvider(ModelNode operation) {
        this.providers.remove(providerNameFromOp(operation));
    }

    public void addSecureDeployment(ModelNode operation, ModelNode model) {
        ModelNode deployment = model.clone();
        String name = deploymentNameFromOp(operation);
        this.secureDeployments.put(name, deployment);
    }

    public void updateSecureDeployment(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        deployment.get(attrName).set(resolvedValue);
    }

    public void removeSecureDeployment(ModelNode operation) {
        String name = deploymentNameFromOp(operation);
        this.secureDeployments.remove(name);
    }

    public void addCredential(ModelNode operation, ModelNode model) {
        ModelNode credentials = credentialsFromOp(operation);
        if (! credentials.isDefined()) {
            credentials = new ModelNode();
        }

        setCredential(operation, model, credentials);
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        deployment.get(CREDENTIALS_JSON_NAME).set(credentials);
    }

    private void setCredential(ModelNode operation, ModelNode model, ModelNode credentials) {
        String credentialName = credentialNameFromOp(operation);
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

    public void removeCredential(ModelNode operation) {
        ModelNode credentials = credentialsFromOp(operation);
        if (!credentials.isDefined()) {
            throw ROOT_LOGGER.cannotRemoveCredential(operation.toString());
        }
        String credentialName = credentialNameFromOp(operation);
        credentials.remove(credentialName);
    }

    public void updateCredential(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode credentials = credentialsFromOp(operation);
        if (!credentials.isDefined()) {
            throw ROOT_LOGGER.cannotUpdateCredential(operation.toString());
        }
        setCredential(operation, resolvedValue, credentials);
    }

    private ModelNode credentialsFromOp(ModelNode operation) {
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        return deployment.get(CREDENTIALS_JSON_NAME);
    }

    public void addRedirectRewriteRule(ModelNode operation, ModelNode model) {
        ModelNode redirectRewritesRules = redirectRewriteRuleFromOp(operation);
        if (!redirectRewritesRules.isDefined()) {
            redirectRewritesRules = new ModelNode();
        }
        String redirectRewriteRuleName = redirectRewriteRule(operation);
        redirectRewritesRules.get(redirectRewriteRuleName).set(model.get(ElytronOidcDescriptionConstants.REPLACEMENT).asString());

        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        deployment.get(REDIRECT_REWRITE_RULE_JSON_NAME).set(redirectRewritesRules);
    }

    public void removeRedirectRewriteRule(ModelNode operation) {
        ModelNode redirectRewritesRules = redirectRewriteRuleFromOp(operation);
        if (!redirectRewritesRules.isDefined()) {
            throw ROOT_LOGGER.cannotRemoveRedirectRuntimeRule(operation.toString());
        }

        String ruleName = credentialNameFromOp(operation);
        redirectRewritesRules.remove(ruleName);
    }

    public void updateRedirectRewriteRule(ModelNode operation, String attrName, ModelNode resolvedValue) {
        ModelNode redirectRewritesRules = redirectRewriteRuleFromOp(operation);
        if (!redirectRewritesRules.isDefined()) {
            throw ROOT_LOGGER.cannotUpdateRedirectRuntimeRule(operation.toString());
        }

        String ruleName = credentialNameFromOp(operation);
        redirectRewritesRules.get(ruleName).set(resolvedValue.get(ElytronOidcDescriptionConstants.REPLACEMENT).asString());
    }

    private ModelNode redirectRewriteRuleFromOp(ModelNode operation) {
        ModelNode deployment = this.secureDeployments.get(deploymentNameFromOp(operation));
        return deployment.get(REDIRECT_REWRITE_RULE_JSON_NAME);
    }

    private String providerNameFromOp(ModelNode operation) {
        return valueFromOpAddress(ElytronOidcDescriptionConstants.PROVIDER, operation);
    }

    private String realmNameFromOp(ModelNode operation) {
        return valueFromOpAddress(ElytronOidcDescriptionConstants.REALM, operation);
    }

    private String deploymentNameFromOp(ModelNode operation) {
        String deploymentName = valueFromOpAddress(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT, operation);

        if (deploymentName == null) throw new RuntimeException("Can't find deployment name in address " + operation);

        return deploymentName;
    }

    private String credentialNameFromOp(ModelNode operation) {
        return valueFromOpAddress(ElytronOidcDescriptionConstants.CREDENTIAL, operation);
    }

    private String redirectRewriteRule(ModelNode operation) {
        return valueFromOpAddress(ElytronOidcDescriptionConstants.REDIRECT_REWRITE_RULE, operation);
    }

    private String valueFromOpAddress(String addrElement, ModelNode operation) {
        return getValueOfAddrElement(operation.get(ADDRESS), addrElement);
    }

    private String getValueOfAddrElement(ModelNode address, String elementName) {
        for (ModelNode element : address.asList()) {
            if (element.has(elementName)) return element.get(elementName).asString();
        }

        return null;
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
