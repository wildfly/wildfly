/*
 * Copyright 2016 Red Hat, Inc.
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
package org.jboss.as.security.elytron;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.security.Constants;
import org.jboss.as.security.SecurityExtension;
import org.jboss.dmr.ModelType;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * Defines a resource that represents an Elytron-compatible realm that will be exported by the security subsystem.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronRealmResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement ELYTRON_REALM_PATH = PathElement.pathElement(Constants.ELYTRON_REALM);

    static final String SECURITY_REALM_CAPABILITY =  "org.wildfly.security.security-realm";

    static final RuntimeCapability<Void> SECURITY_REALM_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(SECURITY_REALM_CAPABILITY, true, SecurityRealm.class)
            .build();

    public static final SimpleAttributeDefinition LEGACY_DOMAIN_NAME =
            new SimpleAttributeDefinitionBuilder(Constants.LEGACY_DOMAIN_NAME, ModelType.STRING, false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new StringLengthValidator(1))
                    .setAllowExpression(false)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[]{LEGACY_DOMAIN_NAME};

    public static final ElytronRealmResourceDefinition INSTANCE = new ElytronRealmResourceDefinition();

    private ElytronRealmResourceDefinition() {
        super(ELYTRON_REALM_PATH, SecurityExtension.getResourceDescriptionResolver(Constants.ELYTRON_REALM),
                new ElytronRealmAdd(SECURITY_REALM_RUNTIME_CAPABILITY, ATTRIBUTES), new ReloadRequiredRemoveStepHandler());
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(LEGACY_DOMAIN_NAME, null, new ReloadRequiredWriteAttributeHandler());
    }
}