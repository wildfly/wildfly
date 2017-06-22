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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} implementation for the core mod-cluster SSL configuration resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModClusterSSLResourceDefinition extends SimpleResourceDefinition {

    static final PathElement PATH = PathElement.pathElement(CommonAttributes.SSL, CommonAttributes.CONFIGURATION);

    public static final SimpleAttributeDefinition KEY_ALIAS = SimpleAttributeDefinitionBuilder.create(CommonAttributes.KEY_ALIAS, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .build();

    public static final SimpleAttributeDefinition PASSWORD = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PASSWORD, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("changeit"))
            .setRestartAllServices()
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .build();

    public static final SimpleAttributeDefinition CERTIFICATE_KEY_FILE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CERTIFICATE_KEY_FILE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode().set(new ValueExpression("${user.home}/.keystore")))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CIPHER_SUITE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CIPHER_SUITE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition PROTOCOL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.PROTOCOL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("TLS"))
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CA_CERTIFICATE_FILE = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CA_CERTIFICATE_FILE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition CA_REVOCATION_URL = SimpleAttributeDefinitionBuilder.create(CommonAttributes.CA_REVOCATION_URL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {
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

    private final List<AccessConstraintDefinition> accessConstraints;

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        // Nothing to transform since 1.4.0
    }

    ModClusterSSLResourceDefinition() {
        super(PATH,
                ModClusterExtension.getResourceDescriptionResolver(CommonAttributes.CONFIGURATION, CommonAttributes.SSL),
                ModClusterAddSSL.INSTANCE,
                new ReloadRequiredRemoveStepHandler()
        );
        this.setDeprecated(ModClusterModel.VERSION_5_0_0.getVersion());
        this.accessConstraints = ModClusterExtension.MOD_CLUSTER_SECURITY_DEF.wrapAsList();
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
