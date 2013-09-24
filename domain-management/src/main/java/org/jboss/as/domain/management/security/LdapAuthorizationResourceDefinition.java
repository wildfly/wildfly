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

package org.jboss.as.domain.management.security;


import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.dmr.ModelNode;

/**
 * {@link ResourceDefinition} for a management security realm's LDAP-based Authorization resource.
 *
 *  @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 *  @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthorizationResourceDefinition extends LdapResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = { CONNECTION };

    private static final LdapAuthorizationValidatingHandler VALIDATION_INSTANCE = new LdapAuthorizationValidatingHandler();
    static final LdapAuthorizationChildRemoveHandler REMOVE_INSTANCE = new LdapAuthorizationChildRemoveHandler();

    public LdapAuthorizationResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.AUTHORIZATION, ModelDescriptionConstants.LDAP),
                ControllerResolver.getResolver("core.management.security-realm.authorization.ldap"),
                new LdapAuthorizationChildAddHandler(true, ATTRIBUTE_DEFINITIONS), new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }


    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(UserIsDnResourceDefintion.INSTANCE);
        resourceRegistration.registerSubModel(UserSearchResourceDefintion.INSTANCE);
        resourceRegistration.registerSubModel(AdvancedUserSearchResourceDefintion.INSTANCE);
        resourceRegistration.registerSubModel(GroupToPrincipalResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(PrincipalToGroupResourceDefinition.INSTANCE);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new SecurityRealmChildWriteAttributeHandler(ATTRIBUTE_DEFINITIONS);
        handler.registerAttributes(resourceRegistration);
    }

    /**
     * Creates an operations that targets the valiadating handler.
     *
     * @param operationToValidate the operation that this handler will validate
     * @return  the validation operation
     */
    private static ModelNode createOperation(final ModelNode operationToValidate) {
        PathAddress pa = PathAddress.pathAddress(operationToValidate.require(ModelDescriptionConstants.OP_ADDR));
        PathAddress realmPA = null;
        for (int i = pa.size() - 1; i > 0; i--) {
            PathElement pe = pa.getElement(i);
            if (ModelDescriptionConstants.AUTHORIZATION.equals(pe.getKey()) && ModelDescriptionConstants.LDAP.equals(pe.getValue())) {
                realmPA = pa.subAddress(0, i + 1);
                break;
            }
        }
        assert realmPA != null : "operationToValidate did not have an address that included a " + ModelDescriptionConstants.AUTHORIZATION + "=" + ModelDescriptionConstants.LDAP;
        return Util.getEmptyOperation("validate-authorization", realmPA.toModelNode());
    }

    static class LdapAuthorizationChildAddHandler extends SecurityRealmChildAddHandler {

        public LdapAuthorizationChildAddHandler(boolean validateAuthorization, AttributeDefinition[] attributeDefinitions) {
            super(false, validateAuthorization, attributeDefinitions);
        }

        @Override
        protected void updateModel(OperationContext context, ModelNode operation) throws OperationFailedException {
            super.updateModel(context, operation);

            ModelNode validateOp = createOperation(operation);
            context.addStep(validateOp, VALIDATION_INSTANCE, Stage.MODEL);
        }
    }

    static class LdapAuthorizationChildRemoveHandler implements OperationStepHandler {

        private LdapAuthorizationChildRemoveHandler() {
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            context.removeResource(PathAddress.EMPTY_ADDRESS);

            ModelNode validateOp = createOperation(operation);
            context.addStep(validateOp, VALIDATION_INSTANCE, Stage.MODEL);
        }
    }

    private static class LdapAuthorizationValidatingHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            Set<String> children = resource.getChildrenNames(ModelDescriptionConstants.USERNAME_TO_DN);
            if (children.size() > 1) {
                String realmName = ManagementUtil.getSecurityRealmName(operation);
                Set<String> invalid = new HashSet<String>(children);
                throw DomainManagementMessages.MESSAGES.multipleUsernameToDnConfigurationsDefined(realmName, invalid);
            }
            children = resource.getChildrenNames(ModelDescriptionConstants.GROUP_SEARCH);
            if (children.size() == 0) {
                String realmName = ManagementUtil.getSecurityRealmName(operation);
                throw DomainManagementMessages.MESSAGES.noGroupSearchDefined(realmName);
            } else if (children.size() > 1) {
                String realmName = ManagementUtil.getSecurityRealmName(operation);
                Set<String> invalid = new HashSet<String>(children);
                throw DomainManagementMessages.MESSAGES.multipleGroupSearchConfigurationsDefined(realmName, invalid);
            }

            context.stepCompleted();

        }
    }

}
