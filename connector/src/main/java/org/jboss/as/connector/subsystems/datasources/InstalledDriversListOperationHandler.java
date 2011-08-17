/**
 *
 */
package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.DriverRegistry;
import org.jboss.as.connector.registry.InstalledDriver;
import static org.jboss.as.connector.subsystems.datasources.Constants.DEPLOYMENT_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_COMPLIANT;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Reads the "installed-drivers" attribute.
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InstalledDriversListOperationHandler implements OperationStepHandler {

    public static final InstalledDriversListOperationHandler INSTANCE = new InstalledDriversListOperationHandler();

    private InstalledDriversListOperationHandler() {
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    ServiceController<?> sc = context.getServiceRegistry(false).getRequiredService(
                            ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
                    DriverRegistry driverRegistry = DriverRegistry.class.cast(sc.getValue());

                    ModelNode result = context.getResult();
                    for (InstalledDriver driver : driverRegistry.getInstalledDrivers()) {
                        ModelNode driverNode = new ModelNode();
                        driverNode.get(DRIVER_NAME).set(driver.getDriverName());
                        if (driver.isFromDeployment()) {
                            driverNode.get(DEPLOYMENT_NAME).set(driver.getDriverName());
                            driverNode.get(DRIVER_MODULE_NAME);
                            driverNode.get(MODULE_SLOT);
                            driverNode.get(DRIVER_XA_DATASOURCE_CLASS_NAME);

                        } else {
                            driverNode.get(DEPLOYMENT_NAME);
                            driverNode.get(DRIVER_MODULE_NAME).set(driver.getModuleName().getName());
                            driverNode.get(MODULE_SLOT).set(
                                    driver.getModuleName() != null ? driver.getModuleName().getSlot() : "");
                            driverNode.get(DRIVER_XA_DATASOURCE_CLASS_NAME).set(
                                    driver.getXaDataSourceClassName() != null ? driver.getXaDataSourceClassName() : "");

                        }
                        driverNode.get(DRIVER_CLASS_NAME).set(driver.getDriverClassName());
                        driverNode.get(DRIVER_MAJOR_VERSION).set(driver.getMajorVersion());
                        driverNode.get(DRIVER_MINOR_VERSION).set(driver.getMinorVersion());
                        driverNode.get(JDBC_COMPLIANT).set(driver.isJdbcCompliant());
                        result.add(driverNode);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        } else {
            context.getResult().set("no metrics available");
        }

        context.completeStep();
    }
}
