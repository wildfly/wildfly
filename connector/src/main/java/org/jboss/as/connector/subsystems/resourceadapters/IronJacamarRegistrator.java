package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.subsystems.common.pool.PoolOperations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

import java.util.EnumSet;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTIES;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONNECTIONDEFINITIONS_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.ADMIN_OBJECT_RO_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.CONFIG_PROPERTIES_RO_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.CONNECTION_DEFINITION_RO_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.FLUSH_ALL_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.FLUSH_IDLE_CONNECTION_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.RESOURCEADAPTER_RO_DESC;
import static org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemProviders.TEST_CONNECTION_DESC;

public class IronJacamarRegistrator {
    private ManagementResourceRegistration parentRegistration;
    private static final EnumSet<OperationEntry.Flag> RUNTIME_ONLY_FLAG = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY);


    public IronJacamarRegistrator(ManagementResourceRegistration subRegistration) {
        this.parentRegistration = subRegistration;
    }

    public void invoke() {
        PathElement ijPe = PathElement.pathElement(Constants.IRONJACAMAR_NAME, Constants.IRONJACAMAR_NAME);
        ManagementResourceRegistration ironJacamarChild;

        try {
            ironJacamarChild = parentRegistration.registerSubModel(new SimpleResourceDefinition(ijPe, ResourceAdaptersSubsystemProviders.IRONJACAMAR_DESC));
        } catch (IllegalArgumentException iae) {
            ironJacamarChild = parentRegistration.getSubModel(PathAddress.pathAddress(ijPe));
        }

        PathElement raPath = PathElement.pathElement(RESOURCEADAPTER_NAME);
        ManagementResourceRegistration raChild;
        try {
            raChild = ironJacamarChild.registerSubModel(new SimpleResourceDefinition(raPath, RESOURCEADAPTER_RO_DESC));
        } catch (IllegalArgumentException iae) {
            raChild = ironJacamarChild.getSubModel(PathAddress.pathAddress(raPath));
        }

        PathElement configPath = PathElement.pathElement(CONFIG_PROPERTIES.getName());
        ManagementResourceRegistration configChild;
        try {
            configChild = raChild.registerSubModel(new SimpleResourceDefinition(configPath, CONFIG_PROPERTIES_RO_DESC));

        } catch (IllegalArgumentException iae) {
            configChild = raChild.getSubModel(PathAddress.pathAddress(configPath));
        }
        configChild.registerReadOnlyAttribute(Constants.CONFIG_PROPERTY_VALUE, null);


        PathElement connDefPath = PathElement.pathElement(CONNECTIONDEFINITIONS_NAME);
        ManagementResourceRegistration connChild;
        try {
            connChild = raChild.registerSubModel(new SimpleResourceDefinition(connDefPath, CONNECTION_DEFINITION_RO_DESC));
        } catch (IllegalArgumentException iae) {
            connChild = raChild.getSubModel(PathAddress.pathAddress(connDefPath));
        }
        connChild.registerOperationHandler("flush-idle-connection-in-pool",
                PoolOperations.FlushIdleConnectionInPool.RA_INSTANCE, FLUSH_IDLE_CONNECTION_DESC, false, RUNTIME_ONLY_FLAG);
        connChild.registerOperationHandler("flush-all-connection-in-pool",
                PoolOperations.FlushAllConnectionInPool.RA_INSTANCE, FLUSH_ALL_CONNECTION_DESC, false, RUNTIME_ONLY_FLAG);
        connChild.registerOperationHandler("test-connection-in-pool", PoolOperations.TestConnectionInPool.RA_INSTANCE,
                TEST_CONNECTION_DESC, false, RUNTIME_ONLY_FLAG);


        PathElement connDefConfigPath = PathElement.pathElement(CONFIG_PROPERTIES.getName());
        ManagementResourceRegistration connDefConfigChild;
        try {
            connDefConfigChild = connChild.registerSubModel(new SimpleResourceDefinition(connDefConfigPath, CONFIG_PROPERTIES_RO_DESC));
        } catch (IllegalArgumentException iae) {
            connDefConfigChild = connChild.getSubModel(PathAddress.pathAddress(connDefConfigPath));
        }
        connDefConfigChild.registerReadOnlyAttribute(Constants.CONFIG_PROPERTY_VALUE, null);


        PathElement adminObjectPath = PathElement.pathElement(ADMIN_OBJECTS_NAME);
        ManagementResourceRegistration adminObjectChild;
        try {
            adminObjectChild = raChild.registerSubModel(new SimpleResourceDefinition(adminObjectPath, ADMIN_OBJECT_RO_DESC));
        } catch (IllegalArgumentException iae) {
            adminObjectChild = raChild.getSubModel(PathAddress.pathAddress(adminObjectPath));
        }

        PathElement adminObjectConfigPath = PathElement.pathElement(CONFIG_PROPERTIES.getName());
        ManagementResourceRegistration adminObjectConfigChild;
        try {
            adminObjectConfigChild = adminObjectChild.registerSubModel(new SimpleResourceDefinition(adminObjectConfigPath, CONFIG_PROPERTIES_RO_DESC));
        } catch (IllegalArgumentException iae) {
            adminObjectConfigChild = adminObjectChild.getSubModel(PathAddress.pathAddress(adminObjectConfigPath));
        }
        adminObjectConfigChild.registerReadOnlyAttribute(Constants.CONFIG_PROPERTY_VALUE, null);



    }
}
