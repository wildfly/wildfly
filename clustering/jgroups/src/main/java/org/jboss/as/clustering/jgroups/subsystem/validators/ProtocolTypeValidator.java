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

package org.jboss.as.clustering.jgroups.subsystem.validators;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jboss.as.clustering.jgroups.subsystem.Protocol;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

// import static org.jboss.as.clustering.MESSAGES;

/**
 * A validator for JGroups protocol types, defined by org.jboss.as.clustering.jgroups.Protocol
 *
 * @author Richard Achmatowicz (c) 2012 Red Hat Inc.
 *
 */
public class ProtocolTypeValidator extends ModelTypeValidator implements AllowedValuesValidator {

    private final EnumSet<Protocol> allowedValues;
    private final List<ModelNode> nodeValues;

    public ProtocolTypeValidator(final boolean nullable) {
        super(ModelType.STRING, nullable, false);
        allowedValues = EnumSet.allOf(Protocol.class);
        nodeValues = new ArrayList<ModelNode>(allowedValues.size());
        for (Protocol protocol : allowedValues) {
            nodeValues.add(new ModelNode().set(protocol.toString()));
        }
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {

        System.out.println("parameter name = " + parameterName + ", value = " + value);

        super.validateParameter(parameterName, value);

        if (value.isDefined()) {
            final Protocol protocol = Protocol.valueOf(value.asString());
            if (protocol == null || !allowedValues.contains(protocol)) {
               // throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidTargetName(allowedValues)));
               throw new OperationFailedException(new ModelNode().set("invalid value for " + protocol));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return nodeValues;
    }
}