/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.threadpool;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.Set;

/**
 * @author Miroslav Novak
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SnapshotRestoreSetupTask.class)
public class Ejb3ThreadPoolModelTestCase extends Ejb3ThreadPoolBase {

    private PathAddress myPoolPathAddress = PathAddress.pathAddress("subsystem", "ejb3").append("thread-pool", "myPool");

    @Test
    public void testCreateRemoveThreadPool() throws Exception {
        int maxThreads = 27;
        int coreThreads = 6;
        long keepAliveTime = 18;
        String keepAliveTimeUnit = "SECONDS";

        // we don't know exact name of thread pool created for ejb3 subsystem in jboss-threads
        // thus we create one and query based on attributes
        // first make sure there is no such thread pool
        final MBeanServerConnection mbs = getMBeanServerConnection();
        Set<ObjectName> objs = mbs.queryNames(new ObjectName("jboss.threads:name=*,type=thread-pool"), null);
        Assert.assertEquals("There is already thread pool with given params but it shouldn't be.", 0,
                objs.stream()
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs, o, "MaximumPoolSize")) == maxThreads)
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs, o, "CorePoolSize")) == coreThreads)
                        .filter((o) -> ((long) readAttributeValueFromMBeanObject(mbs, o, "KeepAliveTimeSeconds")) == keepAliveTime)
                        .count());

        // create one
        ModelNode addThreadPool = Util.createAddOperation(myPoolPathAddress);
        addThreadPool.get("max-threads").set(maxThreads);
        addThreadPool.get("core-threads").set(coreThreads);
        addThreadPool.get("keepalive-time").set(new ModelNode().add("unit", keepAliveTimeUnit)
                .add("time", keepAliveTime));
        executeOperation(addThreadPool);

        final MBeanServerConnection mbs2 = getMBeanServerConnection();
        objs = mbs2.queryNames(new ObjectName("jboss.threads:name=*,type=thread-pool"), null);
        Assert.assertEquals("Thread pool was not created or does not have given params.", 1,
                objs.stream()
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "MaximumPoolSize")) == maxThreads)
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "CorePoolSize")) == coreThreads)
                        .filter((o) -> ((long) readAttributeValueFromMBeanObject(mbs2, o, "KeepAliveTimeSeconds")) == keepAliveTime)
                        .count());
        ModelNode removeThreadPool = Util.createRemoveOperation(myPoolPathAddress);
        executeOperation(removeThreadPool);

    }

    @Test
    public void testWriteReadAttributes() throws Exception {
        int maxThreads = 12;
        int coreThreads = 4;
        long keepAliveTime = 5;
        String keepAliveTimeUnit = "SECONDS";

        // create one
        ModelNode writeMaxThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads", maxThreads);
        ModelNode writeCoreThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads", coreThreads);
        ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", keepAliveTimeUnit)
                        .add("time", keepAliveTime)));

        executeOperation(writeMaxThreadsOp);
        executeOperation(writeCoreThreadsOp);
        executeOperation(writeKeepAliveTimeOp);

        // read from mbean
        final MBeanServerConnection mbs2 = getMBeanServerConnection();
        Set<ObjectName> objs = mbs2.queryNames(new ObjectName("jboss.threads:name=*,type=thread-pool"), null);
        Assert.assertEquals("Parameters were not applied into thread-pool " + DEFAULT_THREAD_POOL_ADDRESS, 1,
                objs.stream()
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "MaximumPoolSize")) == maxThreads)
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "CorePoolSize")) == coreThreads)
                        .filter((o) -> ((long) readAttributeValueFromMBeanObject(mbs2, o, "KeepAliveTimeSeconds")) == keepAliveTime)
                        .count());

        // read from CLI
        ModelNode readMaxThreadsOp = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads");
        ModelNode readCoreThreadsOp = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads");
        ModelNode readKeepAliveOp = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time");
        Assert.assertEquals(maxThreads, executeOperation(readMaxThreadsOp).asInt());
        Assert.assertEquals(coreThreads, executeOperation(readCoreThreadsOp).asInt());
        ModelNode readKeepAliveTime = executeOperation(readKeepAliveOp);
        Assert.assertEquals(readKeepAliveTime.get("time").asLong(), keepAliveTime);
        Assert.assertEquals(readKeepAliveTime.get("unit").asString(), keepAliveTimeUnit);
    }

    @Test(expected = Exception.class)
    public void testNegativeMaxThreadValue() throws Exception {
        ModelNode writeMaxThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads", -12);
        executeOperation(writeMaxThreadsOp);
    }

    @Test(expected = Exception.class)
    public void testNegativeCoreThreadValue() throws Exception {
        ModelNode writeCoreThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads", -12);
        executeOperation(writeCoreThreadsOp);
    }

    @Test(expected = Exception.class)
    public void testNegativeKeepAliveTimeoutValue() throws Exception {
        ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", "SECONDS")
                        .add("time", -12)));
        executeOperation(writeKeepAliveTimeOp);
    }

    @Test(expected = Exception.class)
    public void testIllegalTimeUnitValue() throws Exception {
        ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", "BADUNIT")
                        .add("time", 12)));
        executeOperation(writeKeepAliveTimeOp);
    }

    private MBeanServerConnection getMBeanServerConnection() throws Exception {
        final String address = managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort();
        JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:remote+http://" + address), DefaultConfiguration.credentials());
        return connector.getMBeanServerConnection();
    }

    private Object readAttributeValueFromMBeanObject(MBeanServerConnection mbs, ObjectName o, String attribute) {
        try {
            return mbs.getAttribute(o, attribute);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
