/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.threadpool;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Miroslav Novak
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals(0, objs.stream()
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs, o, "MaximumPoolSize")) == maxThreads)
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs, o, "CorePoolSize")) == coreThreads)
                        .filter((o) -> ((long) readAttributeValueFromMBeanObject(mbs, o, "KeepAliveTimeSeconds")) == keepAliveTime)
                        .count(),
                "There is already thread pool with given params but it shouldn't be.");

        // create one
        ModelNode addThreadPool = Util.createAddOperation(myPoolPathAddress);
        addThreadPool.get("max-threads").set(maxThreads);
        addThreadPool.get("core-threads").set(coreThreads);
        addThreadPool.get("keepalive-time").set(new ModelNode().add("unit", keepAliveTimeUnit)
                .add("time", keepAliveTime));
        executeOperation(addThreadPool);

        final MBeanServerConnection mbs2 = getMBeanServerConnection();
        objs = mbs2.queryNames(new ObjectName("jboss.threads:name=*,type=thread-pool"), null);
        Assertions.assertEquals(1, objs.stream()
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "MaximumPoolSize")) == maxThreads)
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "CorePoolSize")) == coreThreads)
                        .filter((o) -> ((long) readAttributeValueFromMBeanObject(mbs2, o, "KeepAliveTimeSeconds")) == keepAliveTime)
                        .count(),
                "Thread pool was not created or does not have given params.");
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
        Assertions.assertEquals(1, objs.stream()
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "MaximumPoolSize")) == maxThreads)
                        .filter((o) -> ((int) readAttributeValueFromMBeanObject(mbs2, o, "CorePoolSize")) == coreThreads)
                        .filter((o) -> ((long) readAttributeValueFromMBeanObject(mbs2, o, "KeepAliveTimeSeconds")) == keepAliveTime)
                        .count(),
                "Parameters were not applied into thread-pool " + DEFAULT_THREAD_POOL_ADDRESS);

        // read from CLI
        ModelNode readMaxThreadsOp = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads");
        ModelNode readCoreThreadsOp = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads");
        ModelNode readKeepAliveOp = Util.getReadAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time");
        Assertions.assertEquals(maxThreads, executeOperation(readMaxThreadsOp).asInt());
        Assertions.assertEquals(coreThreads, executeOperation(readCoreThreadsOp).asInt());
        ModelNode readKeepAliveTime = executeOperation(readKeepAliveOp);
        Assertions.assertEquals(readKeepAliveTime.get("time").asLong(), keepAliveTime);
        Assertions.assertEquals(readKeepAliveTime.get("unit").asString(), keepAliveTimeUnit);
    }

    @Test
    public void testNegativeMaxThreadValue() {
        assertThrows(Exception.class, () -> {
            ModelNode writeMaxThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "max-threads", -12);
            executeOperation(writeMaxThreadsOp);
        });
    }

    @Test
    public void testNegativeCoreThreadValue() {
        assertThrows(Exception.class, () -> {
            ModelNode writeCoreThreadsOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "core-threads", -12);
            executeOperation(writeCoreThreadsOp);
        });
    }

    @Test
    public void testNegativeKeepAliveTimeoutValue() {
        assertThrows(Exception.class, () -> {
            ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                    new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", "SECONDS")
                            .add("time", -12)));
            executeOperation(writeKeepAliveTimeOp);
        });
    }

    @Test
    public void testIllegalTimeUnitValue() {
        assertThrows(Exception.class, () -> {
            ModelNode writeKeepAliveTimeOp = Util.getWriteAttributeOperation(DEFAULT_THREAD_POOL_ADDRESS, "keepalive-time",
                    new ModelNode().get("keepalive-time").set(new ModelNode().add("unit", "BADUNIT")
                            .add("time", 12)));
            executeOperation(writeKeepAliveTimeOp);
        });
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
