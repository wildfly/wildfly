/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import static org.junit.Assert.assertEquals;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanPermission;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that Service MBeans that depend on other such mbeans have their lifecycle methods called in correct dependency order.
 *
 * @author Brian Stansberry
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ServiceMBeanOrderingTestCase {

    private static final String MBEAN_CREATED = LifecycleEmitter.MBEAN_CREATED;
    private static final String MBEAN_STARTED = LifecycleEmitter.MBEAN_STARTED;
    private static final String MBEAN_STOPPED = LifecycleEmitter.MBEAN_STOPPED;
    private static final String MBEAN_DESTROYED = LifecycleEmitter.MBEAN_DESTROYED;
    private static final String MBEAN_PRE_REGISTERED = LifecycleEmitter.MBEAN_PRE_REGISTERED;
    private static final String MBEAN_POST_REGISTERED = LifecycleEmitter.MBEAN_POST_REGISTERED;
    private static final String MBEAN_PRE_DEREGISTERED = LifecycleEmitter.MBEAN_PRE_DEREGISTERED;
    private static final String MBEAN_POST_DEREGISTERED = LifecycleEmitter.MBEAN_POST_DEREGISTERED;

    private static final String UNMANAGED_SAR_DEPLOYMENT_NAME = "service-mbean-order-test";
    private static final List<String> FORWARD_ORDER = Arrays.asList("A", "B", "C");
    private static final List<String> REVERSE_ORDER = Arrays.asList("C", "B", "A");

    private static final List<String> A_EVENTS = Arrays.asList(MBEAN_PRE_REGISTERED, MBEAN_CREATED, MBEAN_STARTED,
            MBEAN_POST_REGISTERED, MBEAN_PRE_DEREGISTERED, MBEAN_POST_DEREGISTERED,
            MBEAN_STOPPED, MBEAN_DESTROYED);
    private static final List<String> D_EVENTS = Arrays.asList(MBEAN_CREATED, MBEAN_STARTED,
            MBEAN_PRE_REGISTERED, MBEAN_POST_REGISTERED, MBEAN_PRE_DEREGISTERED, MBEAN_POST_DEREGISTERED,
            MBEAN_STOPPED, MBEAN_DESTROYED);

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = ServiceMBeanOrderingTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME, managed = false)
    public static JavaArchive geTestMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "service-mbean-order-test.sar");
        sar.addClasses(LifecycleEmitterMBean.class, LifecycleEmitter.class, CustomLifecycleEmitterMBean.class, CustomLifecycleEmitter.class);
        sar.addAsManifestResource(ServiceMBeanOrderingTestCase.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
        sar.addAsManifestResource(createPermissionsXmlAsset(
                new MBeanPermission(ServiceMBeanOrderingTestCase.class.getPackage().getName() + ".*", "*")),
                "permissions.xml");


        return sar;
    }

    @Deployment
    public static JavaArchive getTestResultMBeanSar() {
        final JavaArchive sar = ShrinkWrap.create(JavaArchive.class, "service-mbean-order-test-result.sar");
        sar.addClasses(LifecycleListenerMBean.class, LifecycleListener.class);
        sar.addAsManifestResource(ServiceMBeanOrderingTestCase.class.getPackage(), "result-jboss-service.xml",
                "jboss-service.xml");
        return sar;
    }

    /**
     * Tests that invocation on a service deployed within a .sar, inside a .ear without an application.xml, is successful.
     *
     * @throws Exception if one occurs when connecting the mbean server and reading the event history
     */
    @Test
    public void testServiceMBeanLifecycleOrder() throws Exception {
        // get mbean server
        try (JMXConnector connector = JMXConnectorFactory.connect(managementClient.getRemoteJMXURL(), DefaultConfiguration.credentials())) {
            // deploy the unmanaged sar
            deployer.deploy(ServiceMBeanOrderingTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME);
            // undeploy it
            deployer.undeploy(ServiceMBeanOrderingTestCase.UNMANAGED_SAR_DEPLOYMENT_NAME);

            Events events = new Events(connector.getMBeanServerConnection());

            // Validate that A's events happen in the expected order for a ServiceMBeanSupport subclass
            assertEquals(A_EVENTS, events.aEvents);
            // Validate that D's events happen in the expected order for a non-ServiceMBeanSupport subclass
            assertEquals(D_EVENTS, events.dEvents);

            // C depends on B depends on A. Validate create/start/stop/destroy reflect that
            assertEquals(FORWARD_ORDER, events.creates);
            assertEquals(FORWARD_ORDER, events.starts);
            assertEquals(REVERSE_ORDER, events.stops);
            assertEquals(REVERSE_ORDER, events.destroys);

            // Confirm A is registered before any dependent is created
            for (Map.Entry<String, Integer> entry : events.createsIndexes.entrySet()) {
                assertTrue(entry.getKey() + " was created before A was registered", entry.getValue() > events.aRegistered);
            }

            // Confirm A is deregistered after any dependent is destroyed
            for (Map.Entry<String, Integer> entry : events.destroysIndexes.entrySet()) {
                assertTrue(entry.getKey() + " was destroyed after A was deregistered", entry.getValue() < events.aDeregistered);
            }
        }

    }

    private static class Events {
        int aRegistered;
        int aStarted;
        int aDeregistered;
        final Map<String, Integer> createsIndexes = new HashMap<>();
        final Map<String, Integer> destroysIndexes = new HashMap<>();
        final List<String> creates = new ArrayList<>();
        final List<String> starts = new ArrayList<>();
        final List<String> stops = new ArrayList<>();
        final List<String> destroys = new ArrayList<>();
        final List<String> aEvents = new ArrayList<>();
        final List<String> dEvents = new ArrayList<>();

        private Events(MBeanServerConnection mBeanServerConnection) throws MalformedObjectNameException, ReflectionException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, IOException {

            ObjectName on = new ObjectName("jboss:name=OrderListener");
            @SuppressWarnings("unchecked")
            List<LifecycleListenerMBean.Tuple> order =
                    (List<LifecycleListenerMBean.Tuple>) mBeanServerConnection.getAttribute(on, "AllEvents");

            int i = 0;
            for (LifecycleListenerMBean.Tuple tuple : order) {
                if ("A".equals(tuple.id)) {
                    if (MBEAN_POST_REGISTERED.equals(tuple.method)) {
                        aRegistered = i;
                    } else if (MBEAN_STARTED.equals(tuple.method)) {
                        aStarted = i;
                    } else if (MBEAN_PRE_DEREGISTERED.equals(tuple.method)) {
                        aDeregistered = i;
                    }
                    aEvents.add(tuple.method);
                } else if (MBEAN_CREATED.equals(tuple.method)) {
                    createsIndexes.put(tuple.id, i);
                } else if (MBEAN_DESTROYED.equals(tuple.method)) {
                    destroysIndexes.put(tuple.id, i);
                }

                if ("D".equals(tuple.id)) {
                    dEvents.add(tuple.method);
                } else if (!"E".equals(tuple.id)) { // ignore D and E in this block as they have no ordering relationship with B and C
                    switch (tuple.method) {
                        case MBEAN_CREATED:
                            creates.add(tuple.id);
                            break;
                        case MBEAN_STARTED:
                            starts.add(tuple.id);
                            break;
                        case MBEAN_STOPPED:
                            stops.add(tuple.id);
                            break;
                        case MBEAN_DESTROYED:
                            destroys.add(tuple.id);
                            break;
                    }
                }

                i++;
            }
        }
    }
}