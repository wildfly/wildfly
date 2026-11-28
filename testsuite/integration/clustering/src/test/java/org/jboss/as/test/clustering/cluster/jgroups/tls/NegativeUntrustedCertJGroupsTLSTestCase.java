/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Test that uses SSL/TLS-secured JGroups communication channel, however, without a shared key.
 * In this case, the nodes should reject the connection from each other and must not form a cluster.
 * <p>
 * Server log will contain a similar log – which is expected as keys are not preshared:
 * <pre>
 * WARN  [org.jgroups.protocols.TCP] (TcpServer.Acceptor[7600]-1,null,null) JGRP000006: 127.0.0.1:7600: failed accepting connection from peer SSLSocket[....]: java.net.SocketException: Socket is closed
 * </pre>
 * <p>
 * This test prevents against a potential breakage in TLS secured transport wiring in JGroups subsystem.
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        TLSServerSetupTask.UntrustedCertSecureJGroupsTransport_TCP_NODE_1_2.class,
})
class NegativeUntrustedCertJGroupsTLSTestCase extends AbstractNegativeJGroupsTLSTestCase {

    private static final String MODULE_NAME = NegativeUntrustedCertJGroupsTLSTestCase.class.getSimpleName();

    NegativeUntrustedCertJGroupsTLSTestCase() {
        super(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createDeployment(MODULE_NAME);
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createDeployment(MODULE_NAME);
    }
}
