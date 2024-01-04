/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.AUTH_SERVER_URL;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CLIENT_ID;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.PROVIDER_URL;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REALM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.RESOURCE;
import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.wildfly.security.http.oidc.Oidc;

final class OidcConfigService {

    private static final String CREDENTIALS_JSON_NAME = "credentials";

    private static final String REDIRECT_REWRITE_RULE_JSON_NAME = "redirect-rewrite-rules";

    private static final OidcConfigService INSTANCE = new OidcConfigService();

    public static OidcConfigService getInstance() {
        return INSTANCE;
    }

    private Map<String, ModelNode> realms = new HashMap<>();
    private Map<String, ModelNode> providers = new HashMap<>();

    // deployments secured with OpenID Connect
    private Map<String, ModelNode> secureDeployments = new HashMap<>();

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
        return getJSON(deploymentName, false);
    }

    public String getJSON(String deploymentName, boolean convertToRealmConfiguration) {
        ModelNode deployment = this.secureDeployments.get(deploymentName);
        return getJSON(deployment, convertToRealmConfiguration);
    }

    public void clear() {
        realms = new HashMap<>();
        providers = new HashMap<>();
        secureDeployments = new HashMap<>();
    }

    private String getJSON(ModelNode deployment) {
        return getJSON(deployment, false);
    }

    private String getJSON(ModelNode deployment, boolean convertToRealmConfiguration) {
        ModelNode json = new ModelNode();
        ModelNode realmOrProvider = null;
        boolean removeProvider = false;
        String realmName = deployment.get(ElytronOidcDescriptionConstants.REALM).asStringOrNull();
        if (realmName != null) {
            realmOrProvider = this.realms.get(realmName);
            json.get(ElytronOidcDescriptionConstants.REALM).set(realmName);
        } else {
            String providerName = deployment.get(ElytronOidcDescriptionConstants.PROVIDER).asStringOrNull();
            if (providerName != null) {
                realmOrProvider = this.providers.get(providerName);
                removeProvider = true;
            }
        }

        // set realm/provider values first, some can be overridden by deployment values
        if (realmOrProvider != null) {
            setJSONValues(json, realmOrProvider, Stream.of(ProviderAttributeDefinitions.ATTRIBUTES).map(AttributeDefinition::getName)::iterator);
        }
        setJSONValues(json, deployment, Stream.concat(SecureDeploymentDefinition.ALL_ATTRIBUTES.stream().map(AttributeDefinition::getName), Stream.of(CREDENTIALS_JSON_NAME, REDIRECT_REWRITE_RULE_JSON_NAME))::iterator);
        if (removeProvider) {
            json.remove(ElytronOidcDescriptionConstants.PROVIDER);
        }

        if (convertToRealmConfiguration) {
            String clientId = json.get(CLIENT_ID).asStringOrNull();
            if (clientId != null) {
                json.remove(CLIENT_ID);
                json.get(RESOURCE).set(clientId);
            }

            String providerUrl = json.get(PROVIDER_URL).asStringOrNull();
            if (providerUrl != null) {
                String[] authServerUrlAndRealm = providerUrl.split(Oidc.SLASH + Oidc.KEYCLOAK_REALMS_PATH);
                json.get(AUTH_SERVER_URL).set(authServerUrlAndRealm[0]);
                json.get(REALM).set(authServerUrlAndRealm[1]);
                json.remove(PROVIDER_URL);
            }
        }
        return json.toJSONString(true);
    }

    private static void setJSONValues(ModelNode json, ModelNode values, Iterable<String> keys) {
        synchronized (values) {
            for (String key : keys) {
                if (values.hasDefined(key)) {
                    json.get(key).set(values.get(key));
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
