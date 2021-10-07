/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.ssl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigureElytronSslContextSetupTask extends CLIServerSetupTask {
    private final String CLIENT_KEYSTORE =
            "src/test/resources/org/wildfly/test/integration/microprofile/reactive/messaging/kafka/client.truststore.p12";
    private final String KEYSTORE_PWD = "clientts";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        Path path = Paths.get(CLIENT_KEYSTORE)
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(path)) {
            throw new IllegalStateException(path.toString());
        }

        NodeBuilder nb = this.builder.node(containerId);
        nb.setup("/subsystem=elytron/key-store=kafka-ssl-test:add(credential-reference={clear-text=%s}, path=%s, type=PKCS12)", KEYSTORE_PWD, CLIENT_KEYSTORE);
        nb.setup("/subsystem=elytron/trust-manager=kafka-ssl-test:add(key-store=kafka-ssl-test)");
        nb.setup("/subsystem=elytron/client-ssl-context=kafka-ssl-test:add(trust-manager=kafka-ssl-test)");

        nb.teardown("/subsystem=elytron/client-ssl-context=kafka-ssl-test:remove");
        nb.teardown("/subsystem=elytron/trust-manager=kafka-ssl-test:remove");
        nb.teardown("/subsystem=elytron/key-store=kafka-ssl-test:remove");

        super.setup(managementClient, containerId);
    }
}
