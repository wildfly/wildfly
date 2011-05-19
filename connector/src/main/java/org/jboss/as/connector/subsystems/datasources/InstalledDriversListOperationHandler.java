/**
 *
 */
package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.DEPLOYMENT_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_COMPLIANT;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.DriverRegistry;
import org.jboss.as.connector.registry.InstalledDriver;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Reads the "installed-drivers" attribute.
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InstalledDriversListOperationHandler implements ModelQueryOperationHandler {

    public static final InstalledDriversListOperationHandler INSTANCE = new InstalledDriversListOperationHandler();

    private InstalledDriversListOperationHandler() {
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        final RuntimeOperationContext runtimeContext = context.getRuntimeContext();
        if (runtimeContext != null) {
            runtimeContext.setRuntimeTask(new RuntimeTask() {

                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    ServiceController<?> sc = context.getServiceRegistry().getRequiredService(
                            ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
                    DriverRegistry driverRegistry = DriverRegistry.class.cast(sc.getValue());

                    ModelNode result = new ModelNode();
                    for (InstalledDriver driver : driverRegistry.getInstalledDrivers()) {
                        ModelNode driverNode = new ModelNode();
                        driverNode.get(DRIVER_NAME).set(driver.getDriverName());
                        if (driver.isFromDeployment()) {
                            driverNode.get(DEPLOYMENT_NAME).set(driver.getDriverName());
                            driverNode.get(DRIVER_MODULE_NAME);
                            driverNode.get(MODULE_SLOT);
                        } else {
                            driverNode.get(DEPLOYMENT_NAME);
                            driverNode.get(DRIVER_MODULE_NAME).set(driver.getModuleName().getName());
                            driverNode.get(MODULE_SLOT).set(driver.getModuleName().getSlot());
                        }
                        driverNode.get(DRIVER_CLASS_NAME).set(driver.getDriverClassName());
                        driverNode.get(DRIVER_XA_DATASOURCE_CLASS_NAME).set(driver.getXaDataSourceClassName());
                        driverNode.get(DRIVER_MAJOR_VERSION).set(driver.getMajorVersion());
                        driverNode.get(DRIVER_MINOR_VERSION).set(driver.getMinorVersion());
                        driverNode.get(JDBC_COMPLIANT).set(driver.isJdbcCompliant());
                        result.add(driverNode);
                    }
                    resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, result);
                }
            });
        }

        resultHandler.handleResultComplete();
        return new BasicOperationResult();
    }
}
