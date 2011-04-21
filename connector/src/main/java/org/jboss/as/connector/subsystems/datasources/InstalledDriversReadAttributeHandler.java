/**
 *
 */
package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.DriverRegistry;
import org.jboss.as.connector.registry.InstalledDriver;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Reads the "installed-drivers" attribute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InstalledDriversReadAttributeHandler implements OperationHandler {

    public static final InstalledDriversReadAttributeHandler INSTANCE = new InstalledDriversReadAttributeHandler();

    private InstalledDriversReadAttributeHandler() {
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        final RuntimeOperationContext runtimeContext = context.getRuntimeContext();
        if (runtimeContext != null) {
            runtimeContext.setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceController<?> sc = context.getServiceRegistry().getRequiredService(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
                    DriverRegistry driverRegistry = DriverRegistry.class.cast(sc.getValue());
                    ModelNode result = new ModelNode();
                    for (InstalledDriver driver : driverRegistry.getInstalledDrivers()) {
                        ModelNode driverNode = new ModelNode();
                        if (driver.isFromDeployment()) {
                            driverNode.get("deployment-name").set(driver.getDeploymentUnitName());
                            driverNode.get("module-name");
                            driverNode.get("module-slot");
                        }
                        else {
                            driverNode.get("deployment-name");
                            driverNode.get("module-name").set(driver.getModuleName().getName());
                            driverNode.get("module-slot").set(driver.getModuleName().getSlot());
                        }
                        driverNode.get("driver-class").set(driver.getDriverClassName());
                        driverNode.get("major-version").set(driver.getMajorVersion());
                        driverNode.get("minor-version").set(driver.getMinorVersion());
                        driverNode.get("jdbc-compliant").set(driver.isJdbcCompliant());
                        result.add(driverNode);
                    }
                    resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, result);
                }});
        }

        resultHandler.handleResultComplete();
        return new BasicOperationResult();
    }

}
