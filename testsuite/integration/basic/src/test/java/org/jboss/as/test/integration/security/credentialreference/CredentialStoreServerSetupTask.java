/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.security.credentialreference;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.security.CredentialReference.CREDENTIAL_REFERENCE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author Martin Simka
 */
public class CredentialStoreServerSetupTask implements ServerSetupTask {
    private static final PathAddress CREDENTIAL_STORE_ADDRESS = PathAddress.pathAddress(SUBSYSTEM, "elytron").append("credential-store", "store001");

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        final ModelControllerClient client = managementClient.getControllerClient();
        createCredentialStore(client);
        createAlias(client);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        final ModelNode removeOperation = Operations.createRemoveOperation(CREDENTIAL_STORE_ADDRESS.toModelNode());
        removeOperation.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
        execute(managementClient.getControllerClient(), removeOperation);

        Files.deleteIfExists(Paths.get(resolveJbossServerDataDir(managementClient.getControllerClient()), "store001.jceks"));
    }

    private void createCredentialStore(final ModelControllerClient client) throws IOException {
        final ModelNode addOperation = Operations.createAddOperation(CREDENTIAL_STORE_ADDRESS.toModelNode());
        addOperation.get("create").set("true");
        addOperation.get("modifiable").set("true");
        addOperation.get("location").set("store001.jceks");
        addOperation.get(RELATIVE_TO).set("jboss.server.data.dir");
        final ModelNode credentialReference = addOperation.get(CREDENTIAL_REFERENCE).setEmptyObject();
        credentialReference.get("clear-text").set("joshua");
        execute(client, addOperation);
    }

    private void createAlias(final ModelControllerClient client) throws IOException {
        final ModelNode addOperation = Operations.createOperation("add-alias", CREDENTIAL_STORE_ADDRESS.toModelNode());
        addOperation.get("alias").set("alias001");
        addOperation.get("secret-value").set("chucknorris");
        execute(client, addOperation);
    }

    private ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
        final ModelNode result = client.execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new RuntimeException(Operations.getFailureDescription(result).asString());
        }
        return result;
    }

    private String resolveJbossServerDataDir(final ModelControllerClient client) throws IOException {
        ModelNode operation = Operations.createOperation("resolve-expression");
        operation.get("expression").set("${jboss.server.data.dir}");
        ModelNode result = execute(client, operation);
        return Operations.readResult(result).asString();
    }
}
