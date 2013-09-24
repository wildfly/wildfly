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
package org.jboss.as.domain.management.security;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.dmr.ModelNode;

/**
 * A {@link OperationStepHandler} to validate the number of authorization definitions for a security realm.
 *
 * With some code borrowed from {@link AuthenticationValidatingHandler}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuthorizationValidatingHandler implements OperationStepHandler {

    static final AuthorizationValidatingHandler INSTANCE = new AuthorizationValidatingHandler();

    /**
     * Creates an operations that targets this handler.
     * @param operationToValidate the operation that this handler will validate
     * @return  the validation operation
     */
    static ModelNode createOperation(final ModelNode operationToValidate) {
        PathAddress pa = PathAddress.pathAddress(operationToValidate.require(ModelDescriptionConstants.OP_ADDR));
        PathAddress realmPA = null;
        for (int i = pa.size() - 1; i > 0; i--) {
            PathElement pe = pa.getElement(i);
            if (ModelDescriptionConstants.SECURITY_REALM.equals(pe.getKey())) {
                realmPA = pa.subAddress(0, i + 1);
                break;
            }
        }
        assert realmPA != null : "operationToValidate did not have an address that included a " + ModelDescriptionConstants.SECURITY_REALM;
        return Util.getEmptyOperation("validate-authorization", realmPA.toModelNode());
    }

    private AuthorizationValidatingHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> children = resource.getChildrenNames(ModelDescriptionConstants.AUTHORIZATION);
        if (children.size() > 1) {
            String realmName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
            Set<String> invalid = new HashSet<String>(children);
            throw DomainManagementMessages.MESSAGES.multipleAuthorizationConfigurationsDefined(realmName, invalid);
        }
        context.stepCompleted();
    }
}
