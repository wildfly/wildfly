/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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
package org.keycloak.subsystem.adapter.extension;

import static org.keycloak.subsystem.adapter.extension.KeycloakExtension.CREDENTIAL_DEFINITION;
import static org.keycloak.subsystem.adapter.extension.KeycloakExtension.REDIRECT_RULE_DEFINITON;

import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines attributes and operations for a secure-deployment.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
abstract class AbstractAdapterConfigurationDefinition extends ModelOnlyResourceDefinition {

    protected static final SimpleAttributeDefinition REALM =
            new SimpleAttributeDefinitionBuilder("realm", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition RESOURCE =
            new SimpleAttributeDefinitionBuilder("resource", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();
    protected static final SimpleAttributeDefinition USE_RESOURCE_ROLE_MAPPINGS =
            new SimpleAttributeDefinitionBuilder("use-resource-role-mappings", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition BEARER_ONLY =
            new SimpleAttributeDefinitionBuilder("bearer-only", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition ENABLE_BASIC_AUTH =
            new SimpleAttributeDefinitionBuilder("enable-basic-auth", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition PUBLIC_CLIENT =
            new SimpleAttributeDefinitionBuilder("public-client", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition TURN_OFF_CHANGE_SESSION =
            new SimpleAttributeDefinitionBuilder("turn-off-change-session-id-on-login", ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.FALSE)
                    .build();
    protected static final SimpleAttributeDefinition TOKEN_MINIMUM_TIME_TO_LIVE =
            new SimpleAttributeDefinitionBuilder("token-minimum-time-to-live", ModelType.INT, true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition MIN_TIME_BETWEEN_JWKS_REQUESTS =
            new SimpleAttributeDefinitionBuilder("min-time-between-jwks-requests", ModelType.INT, true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition PUBLIC_KEY_CACHE_TTL =
            new SimpleAttributeDefinitionBuilder("public-key-cache-ttl", ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();
    protected static final SimpleAttributeDefinition ADAPTER_STATE_COOKIE_PATH =
            new SimpleAttributeDefinitionBuilder("adapter-state-cookie-path", ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    static final List<SimpleAttributeDefinition> DEPLOYMENT_ONLY_ATTRIBUTES = new ArrayList<SimpleAttributeDefinition>();

    static {
        DEPLOYMENT_ONLY_ATTRIBUTES.add(REALM);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(RESOURCE);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(USE_RESOURCE_ROLE_MAPPINGS);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(BEARER_ONLY);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(ENABLE_BASIC_AUTH);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(PUBLIC_CLIENT);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(TURN_OFF_CHANGE_SESSION);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(TOKEN_MINIMUM_TIME_TO_LIVE);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(MIN_TIME_BETWEEN_JWKS_REQUESTS);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(PUBLIC_KEY_CACHE_TTL);
        DEPLOYMENT_ONLY_ATTRIBUTES.add(ADAPTER_STATE_COOKIE_PATH);
    }

    static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList();

    static {
        ALL_ATTRIBUTES.addAll(DEPLOYMENT_ONLY_ATTRIBUTES);
        ALL_ATTRIBUTES.addAll(SharedAttributeDefinitons.ATTRIBUTES);
    }

    static final SimpleAttributeDefinition[] ALL_ATTRIBUTES_ARRAY = ALL_ATTRIBUTES.toArray(new SimpleAttributeDefinition[ALL_ATTRIBUTES.size()]);

    static final Map<String, SimpleAttributeDefinition> XML_ATTRIBUTES = new HashMap<String, SimpleAttributeDefinition>();

    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            XML_ATTRIBUTES.put(def.getXmlName(), def);
        }
    }

    private static final Map<String, SimpleAttributeDefinition> DEFINITION_LOOKUP = new HashMap<String, SimpleAttributeDefinition>();
    static {
        for (SimpleAttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }

    protected AbstractAdapterConfigurationDefinition(String name, SimpleAttributeDefinition[] attributes) {
        super(PathElement.pathElement(name),
                KeycloakExtension.getResourceDescriptionResolver(name),
                new ModelOnlyAddStepHandler(attributes),
                attributes
                );
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return DEFINITION_LOOKUP.get(name);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(CREDENTIAL_DEFINITION);
        resourceRegistration.registerSubModel(REDIRECT_RULE_DEFINITON);
    }

}
