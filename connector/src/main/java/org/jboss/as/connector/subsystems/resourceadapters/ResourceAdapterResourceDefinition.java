/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_PRINCIPAL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_REQUIRED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USERS;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdapterResourceDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOLVER = ResourceAdaptersExtension.getResourceDescriptionResolver(RESOURCEADAPTER_NAME);
    private static final OperationDefinition ACTIVATE_DEFINITION = new SimpleOperationDefinitionBuilder(Constants.ACTIVATE, RESOLVER).build();

    private final boolean readOnly;
    private final boolean runtimeOnlyRegistrationValid;
    private final List<AccessConstraintDefinition> accessConstraints;

    public ResourceAdapterResourceDefinition(boolean readOnly, boolean runtimeOnlyRegistrationValid) {
        super(PathElement.pathElement(RESOURCEADAPTER_NAME), RESOLVER, readOnly ? null : RaAdd.INSTANCE, readOnly ? null : RaRemove.INSTANCE);
        this.readOnly = readOnly;
        this.runtimeOnlyRegistrationValid = runtimeOnlyRegistrationValid;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(ResourceAdaptersExtension.SUBSYSTEM_NAME, RESOURCEADAPTER_NAME);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ACTIVATE_DEFINITION, RaActivate.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attribute : CommonAttributes.RESOURCE_ADAPTER_ATTRIBUTE) {
            if (readOnly) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            } else {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new  ReloadRequiredWriteAttributeHandler(attribute));

            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConfigPropertyResourceDefinition(readOnly ? null : ConfigPropertyAdd.INSTANCE, readOnly ? null : ReloadRequiredRemoveStepHandler.INSTANCE));
        resourceRegistration.registerSubModel(new ConnectionDefinitionResourceDefinition(readOnly, runtimeOnlyRegistrationValid));
        resourceRegistration.registerSubModel(new AdminObjectResourceDefinition(readOnly));
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }


    static void registerTransformers200(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME))
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(false)), STATISTICS_ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, STATISTICS_ENABLED)
                .end();
        ConnectionDefinitionResourceDefinition.registerTransformer200(builder);
    }

    static void registerTransformers120(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, WM_SECURITY_MAPPING_USER, WM_SECURITY_MAPPING_GROUP,
                        WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_USERS, WM_SECURITY_DEFAULT_GROUP,
                        WM_SECURITY_DEFAULT_GROUPS, WM_SECURITY_DEFAULT_PRINCIPAL)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(false)), WM_SECURITY, WM_SECURITY_MAPPING_REQUIRED, STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode("other")), WM_SECURITY_DOMAIN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MODULE, WM_SECURITY, WM_SECURITY_MAPPING_USER, WM_SECURITY_MAPPING_GROUP,
                        WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_USERS, WM_SECURITY_DEFAULT_GROUP,
                        WM_SECURITY_DEFAULT_GROUPS, WM_SECURITY_DEFAULT_PRINCIPAL, WM_SECURITY_MAPPING_REQUIRED,
                        WM_SECURITY_DOMAIN, STATISTICS_ENABLED)
                .end();

        ConnectionDefinitionResourceDefinition.registerTransformer120(builder);
    }

    static void registerTransformers110(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME)).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, WM_SECURITY_MAPPING_USER, WM_SECURITY_MAPPING_GROUP,
                        WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_USERS, WM_SECURITY_DEFAULT_GROUP,
                        WM_SECURITY_DEFAULT_GROUPS, WM_SECURITY_DEFAULT_PRINCIPAL)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(false)), WM_SECURITY, WM_SECURITY_MAPPING_REQUIRED, STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode("other")), WM_SECURITY_DOMAIN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, WM_SECURITY, WM_SECURITY_MAPPING_USER, WM_SECURITY_MAPPING_GROUP,
                        WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_USERS, WM_SECURITY_DEFAULT_GROUP,
                        WM_SECURITY_DEFAULT_GROUPS, WM_SECURITY_DEFAULT_PRINCIPAL, WM_SECURITY_MAPPING_REQUIRED,
                        WM_SECURITY_DOMAIN, STATISTICS_ENABLED)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, Constants.MODULE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MODULE)
                //These used to be non-nillable so reject if undefined
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.BEANVALIDATIONGROUP, Constants.ARCHIVE)
                //Expressions not allowed
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.BEANVALIDATIONGROUP, Constants.ARCHIVE)
                .end();

        ConnectionDefinitionResourceDefinition.registerTransformer110(builder);
    }


}
