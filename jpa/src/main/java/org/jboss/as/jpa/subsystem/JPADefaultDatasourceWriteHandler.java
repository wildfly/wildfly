/**
 *
 */
package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;


/**
 * Attribute write handler for the default-datasource attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JPADefaultDatasourceWriteHandler extends ServerWriteAttributeOperationHandler {

    static final JPADefaultDatasourceWriteHandler INSTANCE = new JPADefaultDatasourceWriteHandler();

    private JPADefaultDatasourceWriteHandler() {
        super(new StringLengthValidator(0, Integer.MAX_VALUE, false, true), new StringLengthValidator(0, Integer.MAX_VALUE, false, false));
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
            String attributeName, final ModelNode newValue, ModelNode currentValue) throws OperationFailedException {

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) {
                    final String dataSourceName = newValue.resolve().asString();
                    final ServiceRegistry registry = context.getServiceRegistry(true);
                    ServiceController<?> sc = registry.getRequiredService(JPAService.SERVICE_NAME);
                    JPAService jpaService = JPAService.class.cast(sc.getValue());
                    String currentDataSourceName = JPAService.getDefaultDataSourceName();
                    jpaService.setDefaultDataSourceName(dataSourceName);
                    if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        jpaService.setDefaultDataSourceName(currentDataSourceName);
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
        return false;
    }

}
