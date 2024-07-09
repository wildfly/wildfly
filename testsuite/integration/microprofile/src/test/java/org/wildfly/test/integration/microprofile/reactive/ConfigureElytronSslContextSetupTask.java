/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigureElytronSslContextSetupTask extends CLIServerSetupTask {

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        Path path = Paths.get(KeystoreUtil.CLIENT_TRUSTSTORE)
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(path)) {
            throw new IllegalStateException(path.toString());
        }

        NodeBuilder nb = this.builder.node(containerId);
        nb.setup("/subsystem=elytron/key-store=kafka-ssl-test:add(credential-reference={clear-text=%s}, path=%s, type=PKCS12)", KeystoreUtil.CLIENT_TRUSTSTORE_PWD, KeystoreUtil.CLIENT_TRUSTSTORE);
        nb.setup("/subsystem=elytron/trust-manager=kafka-ssl-test:add(key-store=kafka-ssl-test)");
        nb.setup("/subsystem=elytron/client-ssl-context=kafka-ssl-test:add(trust-manager=kafka-ssl-test)");

        nb.teardown("/subsystem=elytron/client-ssl-context=kafka-ssl-test:remove");
        nb.teardown("/subsystem=elytron/trust-manager=kafka-ssl-test:remove");
        nb.teardown("/subsystem=elytron/key-store=kafka-ssl-test:remove");

        super.setup(managementClient, containerId);
    }
}
