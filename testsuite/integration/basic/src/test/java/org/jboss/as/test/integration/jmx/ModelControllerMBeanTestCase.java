/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jmx;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.jmx.model.ModelControllerMBeanHelper;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xnio.IoUtils;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ModelControllerMBeanTestCase {

    private static final String RESOLVED_DOMAIN = "jboss.as";

    static final ObjectName RESOLVED_MODEL_FILTER = createObjectName(RESOLVED_DOMAIN  + ":*");
    static final ObjectName RESOLVED_ROOT_MODEL_NAME = ModelControllerMBeanHelper.createRootObjectName(RESOLVED_DOMAIN);

    static JMXConnector connector;
    static MBeanServerConnection connection;

    @ContainerResource
    private ManagementClient managementClient;


    @Before
    public void initialize() throws Exception {
        connection = setupAndGetConnection();
    }

    @After
    public void closeConnection() throws Exception {
        IoUtils.safeClose(connector);
    }

    /**
     * Test that all the MBean infos can be read properly
     */
    @Test
    public void testAllMBeanInfos() throws Exception {
        Set<ObjectName> names = connection.queryNames(RESOLVED_MODEL_FILTER, null);
        Map<ObjectName, Exception> failedInfos = new HashMap<ObjectName, Exception>();

        for (ObjectName name : names) {
            try {
                Assert.assertNotNull(connection.getMBeanInfo(name));
            } catch (Exception e) {
                failedInfos.put(name, e);
            }
        }

        // https://issues.redhat.com/browse/WFLY-13977:
        // There are some MBeans which only live a short time, such as the active operations.
        // They may be returned from the original queryNames() call, and then don't exist
        // in the check so they get added to failedInfos. Remove from failedInfos the ones
        // which are not there in a fresh queryNames() call
        Set<ObjectName> currentNames = connection.queryNames(RESOLVED_MODEL_FILTER, null);
        for (ObjectName name : new HashMap<>(failedInfos).keySet()) {
            if (!currentNames.contains(name)) {
                failedInfos.remove(name);
            }
        }

        Assert.assertTrue(failedInfos.toString(), failedInfos.isEmpty());
    }

    private static ObjectName createObjectName(String name) {
        try {
            return ObjectName.getInstance(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MBeanServerConnection setupAndGetConnection() throws Exception {
        // Make sure that we can connect to the MBean server
        String urlString = System
                .getProperty("jmx.service.url", "service:jmx:remote+http://" + managementClient.getMgmtAddress() + ":" + managementClient.getMgmtPort());
        JMXServiceURL serviceURL = new JMXServiceURL(urlString);
        connector = JMXConnectorFactory.connect(serviceURL, DefaultConfiguration.credentials());
        return connector.getMBeanServerConnection();
    }

}