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

package org.jboss.as.remoting;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Add a connector to a remoting container.
 *
 * @author Kabir Khan
 */
public class SaslPolicyAdd extends AbstractAddStepHandler {

    static final SaslPolicyAdd INSTANCE = new SaslPolicyAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException{
        SaslPolicyResource.FORWARD_SECRECY.validateAndSet(operation, model);
        SaslPolicyResource.NO_ACTIVE.validateAndSet(operation, model);
        SaslPolicyResource.NO_ANONYMOUS.validateAndSet(operation, model);
        SaslPolicyResource.NO_DICTIONARY.validateAndSet(operation, model);
        SaslPolicyResource.NO_PLAIN_TEXT.validateAndSet(operation, model);
        SaslPolicyResource.PASS_CREDENTIALS.validateAndSet(operation, model);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
    }

}
