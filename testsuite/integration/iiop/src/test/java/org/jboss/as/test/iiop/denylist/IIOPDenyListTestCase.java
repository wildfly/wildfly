/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.PropertiesValueResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for the IIOP deny-list filter spec override (jboss.experimental.iiop.deserialization.filter.spec).
 * Runs against the dedicated iiop-denylist-server container, which is the only container whose default deny-list
 * is replaced by the custom filter spec; see IIOPDefaultDenyListTestCase for coverage of the built-in default
 * deny-list, which stays active on the shared iiop-server container.
 * Tests that:
 * 1. Classes added via the custom filter spec are blocked
 * 2. Safe classes are allowed through
 */
@RunWith(Arquillian.class)
public class IIOPDenyListTestCase {

    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-denylist-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "denylist-server.jar");
        jar.addClasses(IIOPDenyListBean.class, IIOPDenyListHome.class, IIOPDenyListRemote.class, DangerousClass.class)
                .addAsManifestResource(IIOPDenyListTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {
        String ejbJar = FileUtils.readFile(IIOPDenyListTestCase.class, "denylist-ejb-jar.xml");

        // IPv6 addresses must be enclosed in brackets to be usable in a corbaname URL
        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "denylist-client.jar");
        jar.addClasses(IIOPDenyListTestCase.class, IIOPDenyListRemote.class, IIOPDenyListHome.class, DangerousClass.class, ClientEjb.class);
        jar.addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testSafeClassIsAllowed() throws Exception {
        final ClientEjb ejb = getClient();
        String result = ejb.invokeSafeMethod("test payload");
        Assert.assertEquals("Accepted safe class: test payload", result);
    }

    @Test
    @OperateOnDeployment("client")
    public void testDangerousClassIsBlocked() throws Exception {
        final ClientEjb ejb = getClient();
        try {
            DangerousClass dangerous = new DangerousClass("malicious payload");
            ejb.invokeDangerousMethod(dangerous);
            Assert.fail("Expected exception due to deny-list filter");
        } catch (Exception e) {
            // Expected - verify it's the filter rejection
            // The exception is wrapped in MarshalException/CORBA MARSHAL, so check the entire exception chain.
            // The ORB always reports a rejection as "Deserialization of class <name> rejected by filter"
            // (see openjdk-orb's IIOPInputStream), so require that exact pairing rather than a loose substring
            // that an unrelated classloading/marshalling failure could also satisfy.
            String fullMessage = getFullExceptionMessage(e);
            String expected = "Deserialization of class " + DangerousClass.class.getName() + " rejected by filter";
            Assert.assertTrue("Exception chain should report filter rejection of DangerousClass, but was: " + fullMessage,
                    fullMessage.contains(expected));
        }
    }

    private String getFullExceptionMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            if (t.getMessage() != null) {
                sb.append(t.getMessage()).append(" ");
            }
            t = t.getCause();
        }
        return sb.toString();
    }

    private ClientEjb getClient() throws NamingException {
        final InitialContext context = new InitialContext();
        return (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
    }
}
