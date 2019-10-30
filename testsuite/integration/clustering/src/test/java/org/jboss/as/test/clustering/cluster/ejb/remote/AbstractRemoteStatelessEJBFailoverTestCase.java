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

package org.jboss.as.test.clustering.cluster.ejb.remote;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;
import java.util.function.UnaryOperator;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SecureStatelessIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * Validates failover behavior of a remotely accessed @Stateless EJB.
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
public abstract class AbstractRemoteStatelessEJBFailoverTestCase extends AbstractClusteringTestCase {

    private static final int COUNT = 20;
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static final long INVOCATION_WAIT = TimeoutUtil.adjust(10);

    static Archive<?> createDeployment(String moduleName) {
        return ShrinkWrap.create(JavaArchive.class, moduleName + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatelessIncrementorBean.class, SecureStatelessIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private final ExceptionSupplier<EJBDirectory, Exception> directoryProvider;
    private final Class<? extends Incrementor> beanClass;
    private final UnaryOperator<Callable<Void>> configurator;

    AbstractRemoteStatelessEJBFailoverTestCase(ExceptionSupplier<EJBDirectory, Exception> directoryProvider, Class<? extends Incrementor> beanClass, UnaryOperator<Callable<Void>> configurator) {
        this.directoryProvider = directoryProvider;
        this.beanClass = beanClass;
        this.configurator = configurator;
    }

    @Test
    public void test() throws Exception {
        this.configurator.apply(() -> {
            try (EJBDirectory directory = this.directoryProvider.get()) {
                Incrementor bean = directory.lookupStateless(this.beanClass, Incrementor.class);

                // Allow sufficient time for client to receive full topology
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                List<String> results = new ArrayList<>(COUNT);
                for (int i = 0; i < COUNT; ++i) {
                    Result<Integer> result = bean.increment();
                    results.add(result.getNode());
                    Thread.sleep(INVOCATION_WAIT);
                }

                for (String node : TWO_NODES) {
                    int frequency = Collections.frequency(results, node);
                    assertTrue(String.valueOf(frequency) + " invocations were routed to " + node, frequency > 0);
                }

                undeploy(DEPLOYMENT_1);

                for (int i = 0; i < COUNT; ++i) {
                    Result<Integer> result = bean.increment();
                    results.set(i, result.getNode());
                    Thread.sleep(INVOCATION_WAIT);
                }

                Assert.assertEquals(0, Collections.frequency(results, NODE_1));
                Assert.assertEquals(COUNT, Collections.frequency(results, NODE_2));

                deploy(DEPLOYMENT_1);

                // Allow sufficient time for client to receive new topology
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                for (int i = 0; i < COUNT; ++i) {
                    Result<Integer> result = bean.increment();
                    results.set(i, result.getNode());
                    Thread.sleep(INVOCATION_WAIT);
                }

                for (String node : TWO_NODES) {
                    int frequency = Collections.frequency(results, node);
                    assertTrue(String.valueOf(frequency) + " invocations were routed to " + node, frequency > 0);
                }

                stop(NODE_2);

                for (int i = 0; i < COUNT; ++i) {
                    Result<Integer> result = bean.increment();
                    results.set(i, result.getNode());
                    Thread.sleep(INVOCATION_WAIT);
                }

                Assert.assertEquals(COUNT, Collections.frequency(results, NODE_1));
                Assert.assertEquals(0, Collections.frequency(results, NODE_2));

                start(NODE_2);

                // Allow sufficient time for client to receive new topology
                Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

                for (int i = 0; i < COUNT; ++i) {
                    Result<Integer> result = bean.increment();
                    results.set(i, result.getNode());
                    Thread.sleep(INVOCATION_WAIT);
                }

                for (String node : TWO_NODES) {
                    int frequency = Collections.frequency(results, node);
                    assertTrue(String.valueOf(frequency) + " invocations were routed to " + node, frequency > 0);
                }
            }
            return null;
        }).call();
    }
}
