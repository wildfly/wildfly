/**
 *
 */
package org.jboss.as.jpa.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.jpa.service.JPAService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;


/**
 * Attribute write handler for the default-datasource attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JPADefaultDatasourceWriteHandler extends AbstractWriteAttributeHandler<String> {

    static final JPADefaultDatasourceWriteHandler INSTANCE = new JPADefaultDatasourceWriteHandler();

    private JPADefaultDatasourceWriteHandler() {
        super(new StringLengthValidator(0, Integer.MAX_VALUE, true, true), new StringLengthValidator(0, Integer.MAX_VALUE, true, false));
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           String attributeName, final ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<String> handbackHolder) throws
        OperationFailedException {

        final String dataSourceName = resolvedValue.asString();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> sc = registry.getRequiredService(JPAService.SERVICE_NAME);
        JPAService jpaService = JPAService.class.cast(sc.getService());
        handbackHolder.setHandback(JPAService.getDefaultDataSourceName());
        jpaService.setDefaultDataSourceName(dataSourceName);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, String handback) throws
        OperationFailedException {

        final ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> sc = registry.getRequiredService(JPAService.SERVICE_NAME);
        JPAService jpaService = JPAService.class.cast(sc.getValue());
        jpaService.setDefaultDataSourceName(handback);
    }

}
