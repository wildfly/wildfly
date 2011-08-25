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

package org.jboss.as.messaging;

import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * {@code OperationStepHandler} for removing a security role.
 *
 * @author Emanuel Muckenhuber
 */
class SecurityRoleRemove extends AbstractRemoveStepHandler implements DescriptionProvider {

    static final SecurityRoleRemove INSTANCE = new SecurityRoleRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final HornetQServer server = getServer(context);
        if(server != null) {
            final String match = address.getElement(address.size() - 2).getValue();
            final String roleName = address.getLastElement().getValue();
            final Set<Role> newRoles = new HashSet<Role>();
            final Set<Role> roles = server.getSecurityRepository().getMatch(match);
            for(final Role role : roles) {
                if(! roleName.equals(role.getName())) {
                     newRoles.add(role);
                }
            }
            server.getSecurityRepository().addMatch(match, newRoles);
        }
    }

    static HornetQServer getServer(final OperationContext context) {
        final ServiceController<?> controller = context.getServiceRegistry(true).getService(MessagingServices.JBOSS_MESSAGING);
        if(controller != null) {
            return HornetQServer.class.cast(controller.getValue());
        }
        return null;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getSecurityRoleRemove(locale);
    }
}
