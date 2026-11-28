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
 * Test that uses SSL/TLS-secured JGroups communication channel, however, TLS is configured on one node only.
 * In this case, the nodes should reject the connection from each other and must not form a cluster.
 * <p>
 * This test prevents against a potential breakage in TLS secured transport wiring in JGroups subsystem.
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        TLSServerSetupTask.PerNodeKeyStore_NODE_1_2.class,
        TLSServerSetupTask.PerNodeSecureJGroupsTransport_TCP_NODE_1.class,
})
class NegativeMixedJGroupsTLSConfigurationTestCase extends AbstractNegativeJGroupsTLSTestCase {

    private static final String MODULE_NAME = NegativeMixedJGroupsTLSConfigurationTestCase.class.getSimpleName();

    NegativeMixedJGroupsTLSConfigurationTestCase() {
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
