package org.jboss.as.test.integration.management;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Stuart Douglas
 */
public abstract class AbstractServerSetupTask implements ServerSetupTask {

    protected void applyUpdate(final ModelControllerClient client, ModelNode update) {
        ModelNode result = null;
        try {
            result = client.execute(new OperationBuilder(update).build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (result.hasDefined("outcome") &&  "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

}
