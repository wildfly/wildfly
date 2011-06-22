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

package org.jboss.as.domain.controller.operations;

import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import org.jboss.as.domain.controller.descriptions.ServerGroupDescription;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerGroupAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final ServerGroupAddHandler INSTANCE = new ServerGroupAddHandler();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(PROFILE).set(operation.require(PROFILE));

        if (operation.hasDefined(SOCKET_BINDING_GROUP)) {
            model.get(SOCKET_BINDING_GROUP).set(operation.get(SOCKET_BINDING_GROUP));
        }

        if (operation.hasDefined(SOCKET_BINDING_PORT_OFFSET)) {
            model.get(SOCKET_BINDING_PORT_OFFSET).set(operation.get(SOCKET_BINDING_PORT_OFFSET));
        }

        if (operation.hasDefined(JVM)) {
            model.get(JVM).set(operation.get(JVM).asString(), new ModelNode());
        } else {
            model.get(JVM);
        }

        model.get(SYSTEM_PROPERTY);
        model.get(DEPLOYMENT);
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerGroupDescription.getServerGroupAdd(locale);
    }

}
