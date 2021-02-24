/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.jpa.config.ExtendedPersistenceInheritance;
import org.jboss.as.jpa.util.JPAServiceNames;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Jakarta Persistence ResourceDefinition
 *
 */
public class JPADefinition extends SimpleResourceDefinition {

    // This a private capability. Its runtime API or capability service tyoe are subject to change.
    // See WFLY-7521
    // Currently it exists to indicate a model dependency from the jpa subsystem to the transactions subsystem
    private static final RuntimeCapability<Void> JPA_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jpa")
            .addRequirements(JPAServiceNames.LOCAL_TRANSACTION_PROVIDER_CAPABILITY)
            .build();

    protected static final SimpleAttributeDefinition DEFAULT_DATASOURCE =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_DATASOURCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(CommonAttributes.DEFAULT_DATASOURCE)
                    .setValidator(new StringLengthValidator(0, Integer.MAX_VALUE, true, true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(CommonAttributes.DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<ExtendedPersistenceInheritance>(ExtendedPersistenceInheritance.class,true,true))
                    .setDefaultValue(new ModelNode(ExtendedPersistenceInheritance.DEEP.toString()))
                    .build();

    public static final JPADefinition INSTANCE = new JPADefinition(true);
    public static final JPADefinition DEPLOYMENT_INSTANCE = new JPADefinition(false);

    private JPADefinition(boolean feature) {
        super(getParameters(feature));
    }

    private static Parameters getParameters(boolean feature) {
        Parameters result = new Parameters(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JPAExtension.SUBSYSTEM_NAME),
                JPAExtension.getResourceDescriptionResolver())
                .setFeature(feature);
        if (feature) {
            result = result.setCapabilities(JPA_CAPABILITY);
        }
        // TODO WFLY-11173 add/remove handlers don't make sense on the deployment resource
        result = result.setAddHandler(JPASubSystemAdd.INSTANCE).setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE);

        return result;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        registration.registerReadWriteAttribute(DEFAULT_DATASOURCE, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_DATASOURCE));
        registration.registerReadWriteAttribute(DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_EXTENDEDPERSISTENCE_INHERITANCE));
    }

    @Override
    public void registerAdditionalRuntimePackages(final ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(
                // Only if annotation is in use.
                RuntimePackageDependency.optional("org.hibernate.search.orm"),
                RuntimePackageDependency.required("org.hibernate"),
                // An alias to org.hibernate module.
                RuntimePackageDependency.optional("org.hibernate.envers"));
    }
}
