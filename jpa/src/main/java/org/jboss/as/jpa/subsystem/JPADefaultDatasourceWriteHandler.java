/**
 *
 */
package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
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
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler,
            String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
        if (context.getRuntimeContext() != null) {
            final String dataSourceName = newValue.resolve().asString();
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry registry = context.getServiceRegistry();
                    ServiceController<?> sc = registry.getRequiredService(JPAService.SERVICE_NAME);
                    JPAService jpaService = JPAService.class.cast(sc.getValue());
                    jpaService.setDefaultDataSourceName(dataSourceName);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return false;
    }

}
