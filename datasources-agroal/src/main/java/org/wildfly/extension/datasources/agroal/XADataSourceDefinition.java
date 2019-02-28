/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.wildfly.extension.datasources.agroal.AgroalExtension.getResolver;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition for the xa-datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class XADataSourceDefinition extends AbstractDataSourceDefinition {

    static final SimpleAttributeDefinition RECOVERY = create("recovery", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition RECOVERY_USERNAME_ATTRIBUTE = create("recovery-username", ModelType.STRING)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAlternatives("recovery-authentication-context")
            .setAllowExpression(true)
            .setRequired(false)
            .setRequires(USERNAME_ATTRIBUTE.getName())
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static final SimpleAttributeDefinition RECOVERY_PASSWORD_ATTRIBUTE = create("recovery-password", ModelType.STRING)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAlternatives("recovery-credential-reference")
            .setAllowExpression(true)
            .setRequired(false)
            .setRequires(RECOVERY_USERNAME_ATTRIBUTE.getName())
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static SimpleAttributeDefinition RECOVERY_AUTHENTICATION_CONTEXT = new SimpleAttributeDefinitionBuilder("recovery-authentication-context", ModelType.STRING, true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_CLIENT_REF)
            .addAlternatives(RECOVERY_USERNAME_ATTRIBUTE.getName())
            .setCapabilityReference(AUTHENTICATION_CONTEXT_CAPABILITY, DATA_SOURCE_CAPABILITY)
            .setRequires(AUTHENTICATION_CONTEXT.getName())
            .setRestartAllServices()
            .build();

    static final ObjectTypeAttributeDefinition RECOVERY_CREDENTIAL_REFERENCE = CredentialReference.getAttributeBuilder("recovery-credential-reference", "recovery-credential-reference", true, true)
                                                                                                  .addAlternatives(RECOVERY_PASSWORD_ATTRIBUTE.getName())
                                                                                                  .build();

    static final Collection<AttributeDefinition> ATTRIBUTES_1_0 = unmodifiableList(asList(JNDI_NAME_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_POOL_ATTRIBUTE));

    static final Collection<AttributeDefinition> ATTRIBUTES_2_0 = unmodifiableList(asList(JNDI_NAME_ATTRIBUTE, STATISTICS_ENABLED_ATTRIBUTE, CONNECTION_FACTORY_ATTRIBUTE, CONNECTION_POOL_ATTRIBUTE, RECOVERY, RECOVERY_USERNAME_ATTRIBUTE, RECOVERY_PASSWORD_ATTRIBUTE, RECOVERY_AUTHENTICATION_CONTEXT, RECOVERY_CREDENTIAL_REFERENCE));

    static final Collection<AttributeDefinition> ATTRIBUTES = ATTRIBUTES_2_0;

    static final XADataSourceDefinition INSTANCE = new XADataSourceDefinition();

    // --- //

    private XADataSourceDefinition() {
        // TODO The cast to PersistentResourceDefinition.Parameters is a workaround to WFCORE-4040
        super((Parameters) new Parameters(pathElement("xa-datasource"), getResolver("xa-datasource"))
                .setAddHandler(XADataSourceOperations.ADD_OPERATION)
                .setRemoveHandler(XADataSourceOperations.REMOVE_OPERATION)
                .setAccessConstraints(new ApplicationTypeAccessConstraintDefinition(
                        new ApplicationTypeConfig(AgroalExtension.SUBSYSTEM_NAME, "xa-datasource"))));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
