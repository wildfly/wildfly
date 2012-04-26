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
package org.jboss.as.osgi.management;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.osgi.parser.ModelConstants;
import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.dmr.ModelNode;

/**
 * Handles changes to the activation attribute.
 *
 * @author David Bosschaert
 */
public class ActivationAttributeHandler implements OperationStepHandler {

    public static final ActivationAttributeHandler INSTANCE = new ActivationAttributeHandler();

    private ActivationAttributeHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Activation val = Activation.valueOf(operation.require(ModelDescriptionConstants.VALUE).asString().toUpperCase(Locale.ENGLISH));
        ModelNode node = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        node.get(ModelConstants.ACTIVATION).set(val.toString().toLowerCase(Locale.ENGLISH));
        context.completeStep();
    }
}
