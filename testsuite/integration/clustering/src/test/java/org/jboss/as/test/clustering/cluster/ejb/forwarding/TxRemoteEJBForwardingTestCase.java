/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.forwarding;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding.ForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Tests concurrent fail-over with a managed transaction context on the forwarder.
 *
 * @author Radoslav Husar
 */
public class TxRemoteEJBForwardingTestCase extends AbstractRemoteEJBForwardingTestCase {

    public static final String MODULE_NAME = TxRemoteEJBForwardingTestCase.class.getSimpleName();

    public TxRemoteEJBForwardingTestCase() {
        super(() -> new RemoteEJBDirectory(MODULE_NAME), ForwardingStatefulSBImpl.class.getSimpleName());
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
