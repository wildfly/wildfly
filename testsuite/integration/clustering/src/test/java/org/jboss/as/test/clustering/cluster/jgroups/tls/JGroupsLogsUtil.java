/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;

/**
 * Util class for JGroups log detection
 */
public class JGroupsLogsUtil {

    /**
     * Try to find WFLYCLJG0037 in last log messages
     */
    public static boolean findWFLYCLJG0037(ManagementClient client) throws Exception {
        // /subsystem=logging/log-file=server.log:read-log-file
        ModelNode operation = new ModelNode();
        operation.get("address").add("subsystem", "logging").add("log-file", "server.log");
        operation.get("operation").set("read-log-file");
        operation.get("lines").set(150);
        operation.get("tail").set(true);
        ModelNode response = client.getControllerClient().execute(operation);
        Assertions.assertTrue("success".equals(response.get("outcome").asString()), "Unable to read logs");
        return response.get("result").asList().stream().anyMatch(lineNode -> lineNode.asString().contains("WFLYCLJG0037"));
    }
}
