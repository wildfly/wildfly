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

package org.jboss.as.server.services.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_OPTIONS;

import java.util.EnumSet;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.ServerMessages;
import org.jboss.dmr.ModelNode;

/**
 * Write attribute handler for attributes that update the core security vault resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class VaultWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final VaultWriteAttributeHandler INSTANCE = new VaultWriteAttributeHandler();

    private VaultWriteAttributeHandler() {
    }

    public void registerAttributes(ManagementResourceRegistration registry) {
        registry.registerReadWriteAttribute(CODE, null, this, EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES));

        registry.registerReadWriteAttribute(VAULT_OPTIONS, null, this, EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES));
    }

    @Override
    protected void validateUnresolvedValue(String name, ModelNode value) throws OperationFailedException {

        if (CODE.equals(name)) {
            VaultAddHandler.codeValidator.validateParameter(VALUE, value);
        } else if (VAULT_OPTIONS.equals(name)) {
            VaultAddHandler.optionsValidator.validateParameter(VALUE, value);
        } else {
            // Bug! Someone added the attribute to the set but did not implement
            throw ServerMessages.MESSAGES.attributeValidationUnimplemented(name);
        }

    }

    @Override
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        // no-op, as we are not going to apply this value until the server is reloaded, so allow
        // any system property to be set between now and then
    }

}
