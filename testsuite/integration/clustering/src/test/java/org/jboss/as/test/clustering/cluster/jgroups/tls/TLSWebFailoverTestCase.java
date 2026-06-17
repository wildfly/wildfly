/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;

import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.web.CoarseWebFailoverTestCase;

/**
 * Variation of the standard {@link CoarseWebFailoverTestCase} that uses TLS/SSL-secured JGroups communication channel.
 *
 * @author Radoslav Husar
 */
@ServerSetup({
        TLSServerSetupTask.PerNodeKeyStore_NODE_1_2_3.class,
        TLSServerSetupTask.PerNodeSecureJGroupsTransport_TCP_NODE_1_2_3.class,
})
public class TLSWebFailoverTestCase extends CoarseWebFailoverTestCase {

    // No changes to the tests are necessary, securing the channel is transparent to the underlying test.

}
