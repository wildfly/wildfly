package org.jboss.as.jdr;

import java.io.IOException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class ModelControllerClientProxy {

    public ModelControllerClient client;

    public ModelControllerClientProxy(ModelControllerClient client) {
        this.client = client;
    }

    public ModelNode execute(ModelNode request) throws java.io.IOException {
        return client.execute(request);
    }
}
