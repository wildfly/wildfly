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

package org.jboss.as.connector.subsystems.common.pool;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;


public class PoolStatisticsRuntimeAttributeWriteHandler implements OperationStepHandler {

    private final StatisticsPlugin stats;

    private final ParametersValidator nameValidator = new ParametersValidator();


    public PoolStatisticsRuntimeAttributeWriteHandler(final StatisticsPlugin stats) {
        this.stats = stats;

    }


    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        nameValidator.validate(operation);
        final String attributeName = operation.require(NAME).asString();
        // Don't require VALUE. Let the validator decide if it's bothered by an undefined value

        ModelNode newValue = operation.hasDefined(VALUE) ? operation.get(VALUE) : new ModelNode();

        final ModelNode resolvedValue = newValue.resolve();

        switch (attributeName) {
            case ModelDescriptionConstants.STATISTICS_ENABLED: {
                stats.setEnabled(resolvedValue.asBoolean());
                break;
            }

        }
    }

}
