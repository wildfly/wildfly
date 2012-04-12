package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.pool.PoolOperations;
import org.jboss.as.controller.PathElement;
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
            ManagementResourceRegistration ironJacamarChild = parentRegistration.registerSubModel(ijPe, ResourceAdaptersSubsystemProviders.IRONJACAMAR_DESC);
            PathElement raPath = PathElement.pathElement(RESOURCEADAPTER_NAME);
            ManagementResourceRegistration raChild = ironJacamarChild.registerSubModel(raPath, RESOURCEADAPTER_RO_DESC);

            PathElement configPath = PathElement.pathElement(CONFIG_PROPERTIES.getName());
            ManagementResourceRegistration configChild = raChild.registerSubModel(configPath, CONFIG_PROPERTIES_RO_DESC);

            PathElement connDefPath = PathElement.pathElement(CONNECTIONDEFINITIONS_NAME);
            ManagementResourceRegistration connChild = raChild.registerSubModel(connDefPath, CONNECTION_DEFINITION_RO_DESC);

            PathElement connDefConfigPath = PathElement.pathElement(CONFIG_PROPERTIES.getName());
            ManagementResourceRegistration connDefConfigChild = connChild.registerSubModel(connDefConfigPath, CONFIG_PROPERTIES_RO_DESC);


            PathElement adminObjectPath = PathElement.pathElement(ADMIN_OBJECTS_NAME);
            ManagementResourceRegistration adminObjectChild = raChild.registerSubModel(adminObjectPath, ADMIN_OBJECT_RO_DESC);

            PathElement adminObjectConfigPath = PathElement.pathElement(CONFIG_PROPERTIES.getName());
            ManagementResourceRegistration adminObjectConfigChild = adminObjectChild.registerSubModel(adminObjectConfigPath, CONFIG_PROPERTIES_RO_DESC);


            connChild.registerOperationHandler("flush-idle-connection-in-pool",
                    PoolOperations.FlushIdleConnectionInPool.RA_INSTANCE, FLUSH_IDLE_CONNECTION_DESC, false, RUNTIME_ONLY_FLAG);
            connChild.registerOperationHandler("flush-all-connection-in-pool",
                    PoolOperations.FlushAllConnectionInPool.RA_INSTANCE, FLUSH_ALL_CONNECTION_DESC, false, RUNTIME_ONLY_FLAG);
            connChild.registerOperationHandler("test-connection-in-pool", PoolOperations.TestConnectionInPool.RA_INSTANCE,
                    TEST_CONNECTION_DESC, false, RUNTIME_ONLY_FLAG);
        }
    }
