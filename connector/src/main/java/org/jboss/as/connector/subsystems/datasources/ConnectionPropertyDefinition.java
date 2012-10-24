package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;

/**
 * @author Stefabo Maestri
 */
public class ConnectionPropertyDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DRIVER = PathElement.pathElement(CONNECTION_PROPERTIES.getName());
    static final ConnectionPropertyDefinition INSTANCE = new ConnectionPropertyDefinition(false);
    static final ConnectionPropertyDefinition DEPLOYED_INSTANCE = new ConnectionPropertyDefinition(true);

    private final boolean deployed;

    private ConnectionPropertyDefinition(final boolean deployed) {
        super(PATH_DRIVER,
                DataSourcesExtension.getResourceDescriptionResolver("data-source", "connection-properties"),
                deployed ? null : ConnectionPropertyAdd.INSTANCE,
                deployed ? null : ConnectionPropertyRemove.INSTANCE);
        this.deployed = deployed;

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_PROPERTY_VALUE).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
            resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLDataSourceRuntimeHandler.INSTANCE);
        } else {
            resourceRegistration.registerReadOnlyAttribute(Constants.CONNECTION_PROPERTY_VALUE, null);
        }

    }

}
