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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link ResourceDefinition} representing a Principal either specified in the include or exclude list of a role mapping.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PrincipalResourceDefinition extends SimpleResourceDefinition {

    public enum Type {
        GROUP, USER
    }

    public static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.TYPE,
            ModelType.STRING, false).setValidator(new EnumValidator<>(Type.class, false, false)).build();

    public static final SimpleAttributeDefinition REALM = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REALM,
            ModelType.STRING, true).build();

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME,
            ModelType.STRING, false).build();

    private PrincipalResourceDefinition(final PathElement pathElement, final OperationStepHandler add,
            final OperationStepHandler remove) {
        super(pathElement, DomainManagementResolver.getResolver("core.access-control.role-mapping.principal"), add, remove);
    }

    static PrincipalResourceDefinition includeResourceDefinition(final WritableAuthorizerConfiguration authorizerConfiguration) {
        return new PrincipalResourceDefinition(PathElement.pathElement(INCLUDE), PrincipalAdd.createForInclude(authorizerConfiguration),
                PrincipalRemove.createForInclude(authorizerConfiguration));
    }

    static PrincipalResourceDefinition excludeResourceDefinition(final WritableAuthorizerConfiguration authorizerConfiguration) {
        return new PrincipalResourceDefinition(PathElement.pathElement(EXCLUDE), PrincipalAdd.createForExclude(authorizerConfiguration),
                PrincipalRemove.createForExclude(authorizerConfiguration));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(TYPE, null);
        resourceRegistration.registerReadOnlyAttribute(REALM, null);
        resourceRegistration.registerReadOnlyAttribute(NAME, null);
    }

    static AuthorizerConfiguration.PrincipalType getPrincipalType(final OperationContext context, final ModelNode model)
            throws OperationFailedException {
        return AuthorizerConfiguration.PrincipalType.valueOf(TYPE.resolveValue(context, model.get(TYPE.getName())).asString());
    }

    static String getRealm(final OperationContext context, final ModelNode model) throws OperationFailedException {
        ModelNode value;
        if ((value = model.get(REALM.getName())).isDefined()) {
            return REALM.resolveValue(context, value).asString();
        } else {
            return null;
        }
    }

    static String getName(final OperationContext context, final ModelNode model) throws OperationFailedException {
        return NAME.resolveValue(context, model.get(NAME.getName())).asString();
    }

}
