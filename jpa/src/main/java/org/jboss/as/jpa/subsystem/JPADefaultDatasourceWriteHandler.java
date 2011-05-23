/**
 *
 */
package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
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
    protected boolean applyUpdateToRuntime(final NewOperationContext context, final ModelNode operation,
            String attributeName, final ModelNode newValue, ModelNode currentValue) throws OperationFailedException {

        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) {
                    final String dataSourceName = newValue.resolve().asString();
                    final ServiceRegistry registry = context.getServiceRegistry(false);
                    ServiceController<?> sc = registry.getRequiredService(JPAService.SERVICE_NAME);
                    JPAService jpaService = JPAService.class.cast(sc.getValue());
                    jpaService.setDefaultDataSourceName(dataSourceName);
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
        return false;
    }

}
