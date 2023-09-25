/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding.ForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.ejb.ClientEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Tests concurrent fail-over with a managed transaction context on the forwarder and using the client "API".
 *
 * @author Radoslav Husar
 */
public class TxClientEJBForwardingTestCase extends AbstractRemoteEJBForwardingTestCase {

    public static final String MODULE_NAME = TxClientEJBForwardingTestCase.class.getSimpleName();

    public TxClientEJBForwardingTestCase() {
        super(() -> new ClientEJBDirectory(MODULE_NAME), ForwardingStatefulSBImpl.class.getSimpleName());
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment1() {
        return createForwardingDeployment(MODULE_NAME, true);
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment2() {
        return createForwardingDeployment(MODULE_NAME, true);
    }
}
