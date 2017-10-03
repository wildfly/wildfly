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
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.twoclusters.bean.SerialBean;
import org.jboss.as.test.clustering.twoclusters.bean.common.CommonStatefulSB;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.AbstractForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.forwarding.NonTxForwardingStatefulSBImpl;
import org.jboss.as.test.clustering.twoclusters.bean.stateful.RemoteStatefulSB;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Tests concurrent fail-over without a managed transaction context on the forwarder.
 *
 * @author Radoslav Husar
 */
public class NonTxRemoteEJBTwoClusterTestCase extends AbstractRemoteEJBTwoClusterTestCase {

    public static final String MODULE_NAME = "clusterbench-ee6-ejb-forwarder";

    public NonTxRemoteEJBTwoClusterTestCase() {
        this(() -> new RemoteEJBDirectory(MODULE_NAME));
    }

    NonTxRemoteEJBTwoClusterTestCase(ExceptionSupplier<EJBDirectory, NamingException> directorySupplier) {
        super(directorySupplier, () -> NonTxForwardingStatefulSBImpl.class);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(CommonStatefulSB.class.getPackage());
        ejbJar.addPackage(RemoteStatefulSB.class.getPackage());
        ejbJar.addClass(SerialBean.class.getName());
        // the forwarding classes
        ejbJar.addClass(AbstractForwardingStatefulSBImpl.class.getName());
        ejbJar.addClass(NonTxForwardingStatefulSBImpl.class.getName());
        // remote outbound connection configuration
        ejbJar.addAsManifestResource(AbstractRemoteEJBTwoClusterTestCase.class.getPackage(), "jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return ejbJar;
    }
}
