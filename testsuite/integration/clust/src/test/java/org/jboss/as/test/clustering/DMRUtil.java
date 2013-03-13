package org.jboss.as.test.clustering;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.junit.Assert;

import org.apache.log4j.Logger;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;

/**
 * @author Ondrej Chaloupka
 */
public class DMRUtil {
    private static final Logger log = Logger.getLogger(DMRUtil.class);

    private static final String IDLE_TIMEOUT_ATTR = "idle-timeout";
    private static final String PASSIVATE_EVENTS_ON_REPLICATE_ATTR = "passivate-events-on-replicate";

    /**
     * Hidden constructor.
     */
    private DMRUtil() {

    }

    /**
     * Returning modelnode address for DRM to be able to set cache attributes (client drm call).
     */
    private static ModelNode getEJB3PassivationStoreAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "ejb3");
        address.add("cluster-passivation-store", "infinispan");
        address.protect();
        return address;
    }

    /**
     * Setting passivation timeout cache attribute (client drm call).
     */
    public static void setPassivationIdleTimeout(ModelControllerClient client) throws Exception {
        ModelNode address = getEJB3PassivationStoreAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get("name").set(IDLE_TIMEOUT_ATTR);
        operation.get("value").set(1L);
        // ModelNode result = client.execute(operation);
        ModelNode result = ManagementOperations.executeOperationRaw(client, operation);
        Assert.assertEquals("Setting of passivation idle timeout attribute was not sucessful", SUCCESS, result.get(OUTCOME).asString());
        log.info("modelnode operation " + WRITE_ATTRIBUTE_OPERATION + " " + IDLE_TIMEOUT_ATTR + " =1: " + result);
    }

    /**
     * Setting on cache replicate attribute (client drm call).
     */
    public static void setPassivationOnReplicate(ModelControllerClient client, boolean value) throws Exception {
        ModelNode address = getEJB3PassivationStoreAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get("name").set(PASSIVATE_EVENTS_ON_REPLICATE_ATTR);
        operation.get("value").set(value);
        // ModelNode result = client.execute(operation);
        ModelNode result = ManagementOperations.executeOperationRaw(client, operation);
        Assert.assertEquals("Setting of passivation on replicate attribute was not sucessful", SUCCESS, result.get(OUTCOME).asString());
        log.info("modelnode operation " + WRITE_ATTRIBUTE_OPERATION + " " + PASSIVATE_EVENTS_ON_REPLICATE_ATTR + " ="
                + (value ? "TRUE" : "FALSE") + ": " + result);
    }

    /**
     * Unsetting specific attribute (client drm call).
     */
    private static void unsetPassivationAttributes(ModelControllerClient client, String attrName) throws Exception {
        ModelNode address = getEJB3PassivationStoreAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get("name").set(attrName);
        ModelNode result = client.execute(operation);
        Assert.assertEquals("Unset of attribute " + attrName + " on server was not sucessful", SUCCESS, result.get(OUTCOME).asString());
        log.info("unset modelnode operation " + UNDEFINE_ATTRIBUTE_OPERATION + " on " + attrName + ": " + result);
    }

    public static void unsetIdleTimeoutPassivationAttribute(ModelControllerClient client) throws Exception {
        unsetPassivationAttributes(client, IDLE_TIMEOUT_ATTR);
    }

    public static void unsetPassivationOnReplicate(ModelControllerClient client) throws Exception {
        unsetPassivationAttributes(client, PASSIVATE_EVENTS_ON_REPLICATE_ATTR);
    }
    
    /**
     * Provide reload operation on server.
     * Until an appropriate API is provided busy waiting is used.
     */
    public static void reload(final ManagementClient managementClient) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");

        try {
            managementClient.getControllerClient().execute(operation);
        } catch (Exception e) {
            log.error("Exception applying reload operation. This is probably fine, as the server probably shut down before the response was sent", e);
        }
        boolean reloaded = false;
        int i = 0;
        while (!reloaded && i++ <= 20) {
            try {
                Thread.sleep(TimeoutUtil.adjust(TimeoutUtil.adjust(2000)));
                if (managementClient.isServerInRunningState()) {
                    reloaded = true;
                }
            } catch (Throwable t) {
                // nothing to do, just waiting
            } 
        }
        if (!reloaded) {
            throw new Exception("Server reloading failed");
        }
    }
}
