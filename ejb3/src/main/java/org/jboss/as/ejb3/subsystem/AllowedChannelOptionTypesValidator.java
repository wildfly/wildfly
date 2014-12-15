/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class AllowedChannelOptionTypesValidator extends ModelTypeValidator implements AllowedValuesValidator {
    public static AllowedChannelOptionTypesValidator INSTANCE = new AllowedChannelOptionTypesValidator();

    private final List<ModelNode> allowedChannelOptTypes;

    private AllowedChannelOptionTypesValidator() {
        super(ModelType.STRING, false);
        allowedChannelOptTypes = new ArrayList<ModelNode>();
        allowedChannelOptTypes.add(new ModelNode().set("remoting"));
        allowedChannelOptTypes.add(new ModelNode().set("xnio"));
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return allowedChannelOptTypes;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            if (!this.allowedChannelOptTypes.contains(value)) {
                throw EjbLogger.ROOT_LOGGER.unknownChannelCreationOptionType(value.asString());
            }
        }
    }
}
