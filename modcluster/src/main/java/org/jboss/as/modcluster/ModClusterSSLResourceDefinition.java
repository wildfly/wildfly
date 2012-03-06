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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} implementation for the core mod-cluster SSL configuration resource.
 *
 * TODO this is a minimal implementation for AS7-3933; finish it off with AS7-4050
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModClusterSSLResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition KEY_ALIAS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.KEY_ALIAS, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PASSWORD = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PASSWORD, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("changeit"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
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

    static final AttributeDefinition[] ATTRIBUTES = {
        KEY_ALIAS, PASSWORD, CERTIFICATE_KEY_FILE, CIPHER_SUITE, PROTOCOL, CA_CERTIFICATE_FILE, CA_REVOCATION_URL
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
        // TODO AS7-4050 Use a correct path and use a ResourceDescriptionResolver; register handlers
        // When doing AS7-4050 note the currently unused ModClusterConfigurationAdd class that should be used or removed
        super(ModClusterExtension.sslConfigurationPath, ModClusterSubsystemDescriptionProviders.SSL);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
         //TODO AS7-4050 get these registered via the constructor
        resourceRegistration.registerOperationHandler(ADD, ModClusterAddSSL.INSTANCE, ModClusterAddSSL.INSTANCE, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
        resourceRegistration.registerOperationHandler(REMOVE, ModClusterRemoveSSL.INSTANCE, ModClusterRemoveSSL.INSTANCE, EnumSet.of(OperationEntry.Flag.RESTART_ALL_SERVICES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }
}
