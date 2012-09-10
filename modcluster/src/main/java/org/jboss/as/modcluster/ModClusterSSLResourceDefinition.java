/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.modcluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} implementation for the core mod-cluster SSL configuration resource.
 * <p/>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModClusterSSLResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition KEY_ALIAS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.KEY_ALIAS, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    @Deprecated
    public static final SimpleAttributeDefinition PASSWORD = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PASSWORD, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(ModelVersion.create(1, 2, 0))
            .build();

    public static final SimpleAttributeDefinition CERTIFICATE_KEY_FILE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CERTIFICATE_KEY_FILE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().setExpression("${user.home}/.keystore"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CIPHER_SUITE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CIPHER_SUITE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROTOCOL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROTOCOL, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("TLS"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CA_CERTIFICATE_FILE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CA_CERTIFICATE_FILE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition CA_REVOCATION_URL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CA_REVOCATION_URL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition TRUSTSTORE_PASSWORD = new SimpleAttributeDefinitionBuilder(CommonAttributes.TRUSTSTORE_PASSWORD, ModelType.STRING)
            .setAllowNull(true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition KEYSTORE_PASSWORD = new SimpleAttributeDefinitionBuilder(CommonAttributes.KEYSTORE_PASSWORD, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("changeit"))
            .setValidator(new StringLengthValidator(1, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
            KEY_ALIAS, CERTIFICATE_KEY_FILE, CIPHER_SUITE, PROTOCOL, CA_CERTIFICATE_FILE, CA_REVOCATION_URL, TRUSTSTORE_PASSWORD, KEYSTORE_PASSWORD
    };

    public static final Map<String, SimpleAttributeDefinition> ATTRIBUTES_BY_NAME;

    static {
        Map<String, SimpleAttributeDefinition> attrs = new HashMap<String, SimpleAttributeDefinition>();
        for (AttributeDefinition attr : ATTRIBUTES) {
            attrs.put(attr.getName(), (SimpleAttributeDefinition) attr);
        }
        ATTRIBUTES_BY_NAME = Collections.unmodifiableMap(attrs);
    }

    public ModClusterSSLResourceDefinition() {
        super(ModClusterExtension.SSL_CONFIGURATION_PATH,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.SSL),
                ModClusterAddSSL.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }
}
