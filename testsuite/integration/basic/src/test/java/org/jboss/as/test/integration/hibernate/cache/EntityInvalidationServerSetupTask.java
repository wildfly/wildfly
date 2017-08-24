package org.jboss.as.test.integration.hibernate.cache;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

/**
 * Creates local-cache for hibernate cache-container and sets values of the lifespan and max-idle attributes
 *
 * @author Daniel Cihak
 */
public class EntityInvalidationServerSetupTask implements ServerSetupTask {

    // /subsystem=infinispan/cache-container=hibernate/local-cache=entity-invalidation
    private static ModelNode CACHE_ADDRESS;
    private static ModelNode EXPIRATION_ADDRESS;

    static {
        CACHE_ADDRESS = new ModelNode();
        CACHE_ADDRESS.add("subsystem", "infinispan");
        CACHE_ADDRESS.add("cache-container", "hibernate");
        CACHE_ADDRESS.add("local-cache", "entity-invalidation");
    }

    @Override
    public void setup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
        final ModelNode cacheAddOperation = new ModelNode();
        cacheAddOperation.get(ADDRESS).set(CACHE_ADDRESS);
        cacheAddOperation.get(OP).set(ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), cacheAddOperation);

        EXPIRATION_ADDRESS = CACHE_ADDRESS.add("component", "expiration");
        System.out.println("EXPIRATION_ADDRESS: " + EXPIRATION_ADDRESS.toJSONString(true));
        final ModelNode lifespanWriteOperation = new ModelNode();
        lifespanWriteOperation.get(ADDRESS).set(EXPIRATION_ADDRESS);
        lifespanWriteOperation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        lifespanWriteOperation.get(NAME).set("lifespan");
        lifespanWriteOperation.get(VALUE).set(300000);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), lifespanWriteOperation);

        final ModelNode maxIdleWriteOperation = new ModelNode();
        maxIdleWriteOperation.get(ADDRESS).set(EXPIRATION_ADDRESS);
        maxIdleWriteOperation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        maxIdleWriteOperation.get(NAME).set("max-idle");
        maxIdleWriteOperation.get(VALUE).set(120000);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), maxIdleWriteOperation);
    }

    @Override
    public void tearDown(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
        final ModelNode cacheRemoveOperation = new ModelNode();
        cacheRemoveOperation.get(ADDRESS).set(CACHE_ADDRESS);
        cacheRemoveOperation.get(OP).set(REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), cacheRemoveOperation);
    }
}
