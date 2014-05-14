/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.mgmt.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.management.Attribute;
import javax.management.JMRuntimeException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.commons.lang.ArrayUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.interfaces.JmxManagementInterface;
import org.jboss.as.test.integration.management.rbac.RbacAdminCallbackHandler;
import org.jboss.as.test.integration.management.rbac.RbacUtil;
import org.jboss.as.test.integration.mgmt.access.dynamic.sar.Dynamic;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public abstract class AbstractJmxNonCoreMBeansSensitivityTestCase {
    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test-deployment.sar")
                .addPackage(Dynamic.class.getPackage())
                .addAsManifestResource(Dynamic.class.getPackage(), "jboss-service.xml", "jboss-service.xml");
    }

    protected abstract boolean isReadAllowed(String userName);

    protected abstract boolean isWriteAllowed(String userName);

    @Test
    public void testMonitor() throws Exception {
        test(RbacUtil.MONITOR_USER);
    }

    @Test
    public void testOperator() throws Exception {
        test(RbacUtil.OPERATOR_USER);
    }

    @Test
    public void testMaintainer() throws Exception {
        test(RbacUtil.MAINTAINER_USER);
    }

    @Test
    public void testDeployer() throws Exception {
        test(RbacUtil.DEPLOYER_USER);
    }

    @Test
    public void testAdministrator() throws Exception {
        test(RbacUtil.ADMINISTRATOR_USER);
    }

    @Test
    public void testAuditor() throws Exception {
        test(RbacUtil.AUDITOR_USER);
    }

    @Test
    public void testSuperUser() throws Exception {
        test(RbacUtil.SUPERUSER_USER);
    }

    private void test(String userName) throws Exception {
        JmxManagementInterface jmx = JmxManagementInterface.create(
                managementClient.getRemoteJMXURL(),
                userName, RbacAdminCallbackHandler.STD_PASSWORD,
                null // not needed, as the only thing from JmxManagementInterface used in this test is getConnection()
        );

        try {
            getAttribute(userName, jmx);
            setAttribute(userName, jmx);

            operationReadOnly(userName, jmx);
            operationWriteOnly(userName, jmx);
            operationReadWrite(userName, jmx);
            operationUnknown(userName, jmx);
        } finally {
            jmx.close();
        }
    }

    // test utils

    private void getAttribute(String userName, JmxManagementInterface jmx) throws Exception {
        boolean successExpected = isReadAllowed(userName);

        MBeanServerConnection connection = jmx.getConnection();
        ObjectName domain = new ObjectName("java.lang:type=OperatingSystem");
        try {
            Object attribute = connection.getAttribute(domain, "Name");
            assertTrue("Failure was expected", successExpected);
            assertEquals(System.getProperty("os.name"), attribute.toString());
        } catch (JMRuntimeException e) {
            if (e.getMessage().contains("WFLYJMX0037")) {
                assertFalse("Success was expected but failure happened: " + e, successExpected);
            } else {
                throw e;
            }
        }
    }

    private void setAttribute(String userName, JmxManagementInterface jmx) throws Exception {
        boolean successExpected = isWriteAllowed(userName);

        MBeanServerConnection connection = jmx.getConnection();
        ObjectName domain = new ObjectName("java.lang:type=Memory");
        try {
            connection.setAttribute(domain, new Attribute("Verbose", true));
            connection.setAttribute(domain, new Attribute("Verbose", false)); // back to default to not pollute the logs
            assertTrue("Failure was expected", successExpected);
        } catch (JMRuntimeException e) {
            if (e.getMessage().contains("WFLYJMX0037")) {
                assertFalse("Success was expected but failure happened: " + e, successExpected);
            } else {
                throw e;
            }
        }
    }

    private void operationReadOnly(String userName, JmxManagementInterface jmx) throws Exception {
        boolean successExpected = isReadAllowed(userName);
        doOperation(successExpected, "helloReadOnly", jmx);
    }

    private void operationWriteOnly(String userName, JmxManagementInterface jmx) throws Exception {
        boolean successExpected = isWriteAllowed(userName);
        doOperation(successExpected, "helloWriteOnly", jmx);
    }

    private void operationReadWrite(String userName, JmxManagementInterface jmx) throws Exception {
        boolean successExpected = isWriteAllowed(userName);
        doOperation(successExpected, "helloReadWrite", jmx);
    }

    private void operationUnknown(String userName, JmxManagementInterface jmx) throws Exception {
        boolean successExpected = isWriteAllowed(userName);
        doOperation(successExpected, "helloUnknown", jmx);
    }

    private void doOperation(boolean successExpected, String operationName, JmxManagementInterface jmx) throws Exception {
        MBeanServerConnection connection = jmx.getConnection();
        ObjectName domain = new ObjectName("jboss.test:service=testdeployments");
        try {
            connection.invoke(domain, operationName, ArrayUtils.EMPTY_OBJECT_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);
            assertTrue("Failure was expected but success happened", successExpected);
        } catch (JMRuntimeException e) {
            if (e.getMessage().contains("WFLYJMX0037")) {
                assertFalse("Success was expected but failure happened: " + e, successExpected);
            } else {
                throw e;
            }
        }
    }
}
