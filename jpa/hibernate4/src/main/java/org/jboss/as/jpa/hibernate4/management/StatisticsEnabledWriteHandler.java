/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

/**
 *
 */
package org.jboss.as.jpa.hibernate4.management;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.jpa.spi.PersistenceUnitServiceRegistry;
import org.jboss.dmr.ModelNode;


/**
 * Attribute write handler for the statistics enabled attribute.
 *
 * @author Scott Marlow
 */
public class StatisticsEnabledWriteHandler implements OperationStepHandler {

    private final ParameterValidator validator = new StringLengthValidator(0, Integer.MAX_VALUE, false, false);
    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;

    public StatisticsEnabledWriteHandler(PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    final ManagementLookup stats;
                    boolean oldSetting = false;
                    {
                        final ModelNode value = operation.get(ModelDescriptionConstants.VALUE).resolve();
                        validator.validateResolvedParameter(ModelDescriptionConstants.VALUE, value);
                        final boolean setting = value.asBoolean();


                        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
                        final String puName = address.getLastElement().getValue();
                        stats = ManagementLookup.create(persistenceUnitRegistry, puName);
                        if (stats != null) {
                            oldSetting = stats.getStatistics().isStatisticsEnabled();
                            stats.getStatistics().setStatisticsEnabled(setting);
                        }
                    }
                    final boolean rollBackValue = oldSetting;
                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            if (stats != null) {
                                stats.getStatistics().setStatisticsEnabled(rollBackValue);
                            }
                        }
                    });

                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.stepCompleted();
    }
}
