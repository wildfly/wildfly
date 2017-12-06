/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.twoclusters;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.NamingEJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSB;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.AbstractForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.ForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Tests concurrent fail-over with a managed transaction context on the forwarder.
 *
 * @author Radoslav Husar
 */
public class TxRemoteEJBTwoClusterTestCase extends AbstractRemoteEJBTwoClusterTestCase {

    public static final String MODULE_NAME = "twocluster-forwarder-tx";

    public TxRemoteEJBTwoClusterTestCase() {
        this(() -> new RemoteEJBDirectory(MODULE_NAME));
    }

    TxRemoteEJBTwoClusterTestCase(ExceptionSupplier<EJBDirectory, NamingException> directorySupplier) {
        super(directorySupplier, ForwardingStatefulSBImpl.class.getSimpleName());
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createForwardingDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createForwardingDeployment();
    }

    private static Archive<?> createForwardingDeployment() {
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addClass(CommonStatefulSB.class);
        ejbJar.addClass(RemoteStatefulSB.class);
        // the forwarding classes
        ejbJar.addClass(AbstractForwardingStatefulSBImpl.class);
        ejbJar.addClass(ForwardingStatefulSBImpl.class);
        ejbJar.addClasses(EJBDirectory.class, NamingEJBDirectory.class, RemoteEJBDirectory.class);
        // remote outbound connection configuration
        ejbJar.addAsManifestResource(AbstractRemoteEJBTwoClusterTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return ejbJar;
    }
}
