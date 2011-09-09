/**
 *
 */
package org.jboss.as.jpa.hibernate4.management;

import org.hibernate.stat.Statistics;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;


/**
 * Attribute write handler for the statistics enabled attribute.
 *
 * @author Scott Marlow
 */
public class StatisticsEnabledWriteHandler extends ServerWriteAttributeOperationHandler {

    static final StatisticsEnabledWriteHandler INSTANCE = new StatisticsEnabledWriteHandler();

    private StatisticsEnabledWriteHandler() {
        super(new StringLengthValidator(0, Integer.MAX_VALUE, false, true), new StringLengthValidator(0, Integer.MAX_VALUE, false, false));
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
            String attributeName, final ModelNode newValue, ModelNode currentValue) throws OperationFailedException {

        if (context.getType() == OperationContext.Type.SERVER) {

            final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
            final Resource jpa = context.getRootResource().navigate(address.subAddress(0, address.size() - 1));
            final ModelNode subModel = jpa.getModel();

            final ModelNode node = jpa.requireChild(address.getLastElement()).getModel();
            final String puname = node.require("scoped-unit-name").asString();

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    final boolean setting = newValue.resolve().asBoolean();
                    Statistics stats = ManagementUtility.getStatistics(context, puname);
                    if (stats != null) {
                        stats.setStatisticsEnabled(setting);

                        if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        // stats.setStatisticsEnabled(false);
                        // what does rollback mean?  revert to previous value?
                        // or revert to false?
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        return false;
    }

}
