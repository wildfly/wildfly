package org.jboss.as.test.clustering;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.junit.Assert;

import org.apache.log4j.Logger;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;

/**
 * @author Ondrej Chaloupka
 */
public class DMRUtil {
    private static final Logger log = Logger.getLogger(DMRUtil.class);

    private static final String MAX_SIZE_ATTRIBUTE = "max-size";

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
        address.add("passivation-store", "infinispan");
        address.protect();
        return address;
    }

    /**
     * Setting max size cache attribute (client drm call).
     */
    public static void setMaxSize(ModelControllerClient client, int maxSize) throws Exception {
        ModelNode address = getEJB3PassivationStoreAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).set(address);
        operation.get("name").set(MAX_SIZE_ATTRIBUTE);
        operation.get("value").set(maxSize);
        // ModelNode result = client.execute(operation);
        ModelNode result = ManagementOperations.executeOperationRaw(client, operation);
        Assert.assertEquals("Setting of max-size attribute was not successful", SUCCESS, result.get(OUTCOME).asString());
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
        Assert.assertEquals("Unset of attribute " + attrName + " on server was not successful", SUCCESS, result.get(OUTCOME).asString());
        log.trace("unset modelnode operation " + UNDEFINE_ATTRIBUTE_OPERATION + " on " + attrName + ": " + result);
    }

    public static void unsetMaxSizeAttribute(ModelControllerClient client) throws Exception {
        unsetPassivationAttributes(client, MAX_SIZE_ATTRIBUTE);
    }
}
