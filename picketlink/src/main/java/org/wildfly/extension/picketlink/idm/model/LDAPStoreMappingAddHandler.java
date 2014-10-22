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
package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.UniqueTypeValidationStepHandler;

import static org.wildfly.extension.picketlink.idm.model.LDAPStoreMappingResourceDefinition.getMappingType;

/**
 * @author Pedro Igor
 */
public class LDAPStoreMappingAddHandler extends IDMConfigAddStepHandler {

    LDAPStoreMappingAddHandler(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void doValidateOnModelStage(OperationContext context, ModelNode operation) {
        context.addStep(new UniqueTypeValidationStepHandler(ModelElement.LDAP_STORE_MAPPING) {
            @Override
            protected String getType(OperationContext context, ModelNode model) throws OperationFailedException {
                return getMappingType(context, model);
            }
        }, OperationContext.Stage.MODEL);

    }
}
