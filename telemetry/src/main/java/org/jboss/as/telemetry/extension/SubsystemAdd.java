package org.jboss.as.telemetry.extension;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
//import org.jboss.as.jdr.JdrReport;
import org.jboss.as.jdr.JdrReportCollector;
import org.jboss.as.jdr.JdrReportService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Handler responsible for adding the subsystem resource to the model
 *
 * @author <a href="jkinlaw@jboss.com">Josh Kinlaw</a>
 */
class SubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final SubsystemAdd INSTANCE = new SubsystemAdd();

    private SubsystemAdd() {
    }

    /** {@inheritDoc} */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        TelemetrySubsystemDefinition.FREQUENCY.validateAndSet(operation, model);
        TelemetrySubsystemDefinition.ENABLED.validateAndSet(operation, model);
    }

    /** {@inheritDoc} */
    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {

        long frequency = TelemetrySubsystemDefinition.FREQUENCY.resolveModelAttribute(context, model).asLong();
        boolean enabled = TelemetrySubsystemDefinition.ENABLED.resolveModelAttribute(context, model).asBoolean();
        TelemetryService service = TelemetryService.getInstance(frequency, enabled);
        ServiceName name = TelemetryService.createServiceName();
        ServiceRegistry registry = context.getServiceRegistry(false);
        JdrReportCollector jdrCollector = JdrReportCollector.class.cast(registry.getRequiredService(JdrReportService.SERVICE_NAME).getValue());
        service.setJdrReportCollector(jdrCollector);
        context.getServiceTarget()
                .addService(name, service)
                .setInitialMode(Mode.ACTIVE)
                .install();
    }
}
