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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.access.rbac.ConfigurableRoleMapper;
import org.jboss.as.controller.access.rbac.ConfigurableRoleMapper.MatchType;
import org.jboss.as.controller.access.rbac.ConfigurableRoleMapper.PrincipalType;
import org.jboss.dmr.ModelNode;

/**
 * A {@link OperationStepHandler} for adding principals to the include / exclude list.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PrincipalAdd implements OperationStepHandler {

    private final ConfigurableRoleMapper roleMapper;
    private final ConfigurableRoleMapper.MatchType matchType;

    private PrincipalAdd(final ConfigurableRoleMapper roleMapper, final ConfigurableRoleMapper.MatchType matchType) {
        this.roleMapper = roleMapper;
        this.matchType = matchType;
    }

    public static OperationStepHandler createForInclude(final ConfigurableRoleMapper roleMapper) {
        return new PrincipalAdd(roleMapper, MatchType.INCLUDE);
    }

    public static OperationStepHandler createForExclude(final ConfigurableRoleMapper roleMapper) {
        return new PrincipalAdd(roleMapper, MatchType.EXCLUDE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        ModelNode model = context.createResource(PathAddress.EMPTY_ADDRESS).getModel();

        // TODO - Somewhere we need to check that these 3 values combined are unique.
        PrincipalResourceDefinition.TYPE.validateAndSet(operation, model);
        PrincipalResourceDefinition.REALM.validateAndSet(operation, model);
        PrincipalResourceDefinition.NAME.validateAndSet(operation, model);

        final String roleName = RoleMappingResourceDefinition.getRoleName(operation);
        final PrincipalType principalType = PrincipalResourceDefinition.getPrincipalType(context, model);
        final String realm = PrincipalResourceDefinition.getRealm(context, model);
        final String name = PrincipalResourceDefinition.getName(context, model);

        /*
         * The address of the resource whilst hopefully being related to the attributes of the Principal resource is not
         * guaranteed, a unique name is needed but not one attribute can be regarded as being suitable as a unique key.
         */
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                roleMapper.addPrincipal(roleName, principalType, matchType, name, realm, context.isBooting());
                context.stepCompleted();
            }
        }, Stage.RUNTIME);

        context.stepCompleted();
    }



}
