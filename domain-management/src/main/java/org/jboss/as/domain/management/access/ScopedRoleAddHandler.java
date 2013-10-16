/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.access;

import org.jboss.as.controller.ParameterCorrector;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * An extension so {@link AbstractAddStepHandler} to add verification that a scoped role is not a duplicate entry.
 *
 * Within the model scoped roles are added using case sensitive addresses, in addition to this the roles can be added as host
 * scoped roles OR server group scoped roles so the additional verification checks for duplicates.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
abstract class ScopedRoleAddHandler extends AbstractAddStepHandler {

    private static final PathAddress AUTHZ_ADDRESS = PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT,
            AccessAuthorizationResourceDefinition.PATH_ELEMENT);
    private final WritableAuthorizerConfiguration authorizerConfiguration;

    ScopedRoleAddHandler(final WritableAuthorizerConfiguration authorizerConfiguration, AttributeDefinition... attributes) {
        super(enhanceAttributes(authorizerConfiguration, attributes));
        this.authorizerConfiguration = authorizerConfiguration;
    }

    private static Collection<AttributeDefinition> enhanceAttributes(
            final WritableAuthorizerConfiguration authorizerConfiguration, AttributeDefinition... attributes) {
        List<AttributeDefinition> enhanced = new ArrayList<AttributeDefinition>(attributes.length);
        for (AttributeDefinition current : attributes) {
            if (current.getName().equals(ModelDescriptionConstants.BASE_ROLE)) {
                assert current instanceof SimpleAttributeDefinition;
                enhanced.add(new SimpleAttributeDefinitionBuilder((SimpleAttributeDefinition)current)
                .setValidator(new ParameterValidator() {
                    @Override
                    public void validateResolvedParameter(String parameterName, ModelNode value)
                            throws OperationFailedException {
                        validateParameter(parameterName, value);
                    }

                    @Override
                    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                        Set<String> standardRoles = authorizerConfiguration.getStandardRoles();
                        String specifiedRole = value.asString();
                        for (String current : standardRoles) {
                            if (specifiedRole.equalsIgnoreCase(current)) {
                                return;
                            }
                        }

                        throw MESSAGES.badBaseRole(specifiedRole);
                    }
                }).setCorrector(new ParameterCorrector() {
                    @Override
                    public ModelNode correct(ModelNode newValue, ModelNode currentValue) {
                        Set<String> standardRoles = authorizerConfiguration.getStandardRoles();
                        String specifiedRole = newValue.asString();

                        for (String current : standardRoles) {
                            if (specifiedRole.equalsIgnoreCase(current) && specifiedRole.equals(current) == false) {
                                return new ModelNode(current);
                            }
                        }

                        return newValue;
                    }
                }).build());
            } else {
                enhanced.add(current);
            }
        }

        return enhanced;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathElement lastElement = address.getLastElement();
        final String roleName = lastElement.getValue();

        Set<String> standardRoles = authorizerConfiguration.getStandardRoles();
        for (String current : standardRoles) {
            if (roleName.equalsIgnoreCase(current)) {
                throw MESSAGES.scopedRoleStandardName(roleName, current);
            }
        }

        Resource readResource = context.readResourceFromRoot(AUTHZ_ADDRESS, false);
        Set<String> hostScopedRoles = readResource.getChildrenNames(HOST_SCOPED_ROLE);
        for (String current : hostScopedRoles) {
            if (roleName.equalsIgnoreCase(current)) {
                throw MESSAGES.duplicateScopedRole(HOST_SCOPED_ROLE, roleName);
            }
        }

        Set<String> serverGroupScopedRoles = readResource.getChildrenNames(SERVER_GROUP_SCOPED_ROLE);
        for (String current : serverGroupScopedRoles) {
            if (roleName.equalsIgnoreCase(current)) {
                throw MESSAGES.duplicateScopedRole(SERVER_GROUP_SCOPED_ROLE, roleName);
            }
        }

        super.execute(context, operation);
    }

}
