/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.ejb;

import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.LocalEJBDirectory;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessDDIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The purpose of this testcase is to ensure that a EJB marked as clustered, either via annotation or deployment
 * descriptor, deploys successfully in a non-clustered environment.
 * This test does <b>not</b> check any clustering semantics (like failover, replication etc...)
 *
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public class ClusteredBeanDeploymentTestCase {

    private static final String MODULE = "clustered-ejb-deployment";
    private static final int COUNT = 5;

    @Deployment
    public static Archive<?> createDDBasedDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar");
        ejbJar.addPackage(Incrementor.class.getPackage());
        ejbJar.addAsManifestResource(Incrementor.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return ejbJar;
    }

    /**
     * Test that a bean marked as clustered via deployment descriptor, deploys fine and is invokable.
     * This test does <b>not</b> test any clustering semantics (like failover etc...)
     *
     * @throws Exception
     */
    @Test
    public void testDDBasedClusteredBeanDeployment() throws Exception {
        testDeployment(StatelessIncrementorBean.class);
    }

    /**
     * Test that a bean marked as clustered via annotation, deploys fine and is invokable. This test does <b>not</b>
     * test any clustering semantics (like failover etc...)
     *
     * @throws Exception
     */
    @Test
    public void testAnnotationBasedClusteredBeanDeployment() throws Exception {
        testDeployment(StatelessDDIncrementorBean.class);
    }

    private static void testDeployment(Class<? extends Incrementor> beanClass) throws NamingException {
        try (EJBDirectory directory = new LocalEJBDirectory(MODULE)) {
            final Incrementor incrementor = directory.lookupStateless(beanClass, Incrementor.class);
            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = incrementor.increment();
                Assert.assertEquals(i + 1, result.getValue().intValue());
            }
        }
    }
}
