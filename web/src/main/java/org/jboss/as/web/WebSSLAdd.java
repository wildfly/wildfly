/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.web.WebSSLDefinition.SSL_ATTRIBUTES;

/**
 * {@code OperationHandler} responsible for defining the SSL entry.
 *
 * @author Jean-Frederic Clere
 */
class WebSSLAdd extends RestartParentResourceAddHandler {

    static final WebSSLAdd INSTANCE = new WebSSLAdd();

    private WebSSLAdd() {
        super(Constants.CONNECTOR);
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        WebSSLDefinition.NAME.validateAndSet(operation, model);

        for (AttributeDefinition def : SSL_ATTRIBUTES){
            def.validateAndSet(operation,model);
        }

    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        WebConnectorAdd.INSTANCE.launchServices(context, parentAddress, parentModel, verificationHandler, null);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        return WebSubsystemServices.JBOSS_WEB_CONNECTOR.append(parentAddress.getLastElement().getValue());
    }
}
