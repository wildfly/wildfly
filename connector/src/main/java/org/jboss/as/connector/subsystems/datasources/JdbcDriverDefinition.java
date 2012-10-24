package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;

/**
 * Stefano Maestri
 */
public class JdbcDriverDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DRIVER = PathElement.pathElement(JDBC_DRIVER_NAME);
    static final JdbcDriverDefinition INSTANCE = new JdbcDriverDefinition();

    private JdbcDriverDefinition() {
        super(PATH_DRIVER,
                DataSourcesExtension.getResourceDescriptionResolver(JDBC_DRIVER_NAME),
                JdbcDriverAdd.INSTANCE,
                JdbcDriverRemove.INSTANCE);

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_MAJOR_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_MINOR_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_DATASOURCE_CLASS_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_XA_DATASOURCE_CLASS_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_CLASS_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DRIVER_MODULE_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.DEPLOYMENT_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.JDBC_COMPLIANT, null);
        resourceRegistration.registerReadOnlyAttribute(Constants.MODULE_SLOT, null);

    }

}
