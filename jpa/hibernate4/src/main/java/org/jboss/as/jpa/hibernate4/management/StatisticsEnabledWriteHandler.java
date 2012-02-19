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

                    ManagementLookup stats = null;
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

                    if (context.completeStep() != OperationContext.ResultAction.KEEP && stats != null) {
                        stats.getStatistics().setStatisticsEnabled(oldSetting);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }

        context.completeStep();
    }
}
