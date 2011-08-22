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
package org.jboss.as.osgi.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.osgi.parser.CommonAttributes.ACTIVATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION;
import static org.jboss.as.osgi.parser.CommonAttributes.MODULE;
import static org.jboss.as.osgi.parser.CommonAttributes.PROPERTY;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Descibe and handle subsystem operations.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
class OSGiSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
    static final OSGiSubsystemDescribeHandler INSTANCE = new OSGiSubsystemDescribeHandler();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode model = context.readModel(PathAddress.EMPTY_ADDRESS);

        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());

        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).set(rootAddress.toModelNode());
        if (model.has(ACTIVATION)) {
            subsystem.get(ACTIVATION).set(model.get(ACTIVATION));
        }
        ModelNode result = context.getResult();
        result.add(subsystem);

        if (model.has(CONFIGURATION)) {
            for(Property conf : model.get(CONFIGURATION).asPropertyList()) {
                ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CONFIGURATION, conf.getName());
                result.add(OSGiCasConfigAdd.getAddOperation(address, conf.getValue()));
            }
        }

        if (model.has(PROPERTY)) {
            for (Property prop : model.get(PROPERTY).asPropertyList()) {
                ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.PROPERTY, prop.getName());
                result.add(OSGiPropertyAdd.getAddOperation(address, prop.getValue()));
            }
        }

        if (model.has(MODULE)) {
            for (Property prop : model.get(MODULE).asPropertyList()) {
                ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.MODULE, prop.getName());
                result.add(OSGiModuleAdd.getAddOperation(address, prop.getValue()));
            }
        }

        context.completeStep();
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getSubsystemDescribeOperation(locale);
    }
}