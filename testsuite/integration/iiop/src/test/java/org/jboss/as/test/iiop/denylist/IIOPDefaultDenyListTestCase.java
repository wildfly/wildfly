/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.swing.UIDefaults;

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
 * Test case for the ORB's built-in default deserialization deny-list, run against the shared iiop-server
 * container whose default filter is left untouched (unlike iiop-denylist-server; see IIOPDenyListTestCase).
 * Uses javax.swing.UIDefaults, a class that is in the default filter spec and trivially instantiable/serializable
 * without any extra test dependency, to prove the built-in deny-list itself still blocks known classes.
 */
@RunWith(Arquillian.class)
public class IIOPDefaultDenyListTestCase {

    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "denylist-default-server.jar");
        jar.addClasses(IIOPDenyListBean.class, IIOPDenyListHome.class, IIOPDenyListRemote.class, DangerousClass.class)
                .addAsManifestResource(IIOPDefaultDenyListTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {
        String ejbJar = FileUtils.readFile(IIOPDefaultDenyListTestCase.class, "ejb-jar.xml");

        // IPv6 addresses must be enclosed in brackets to be usable in a corbaname URL
        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "denylist-default-client.jar");
        jar.addClasses(IIOPDefaultDenyListTestCase.class, IIOPDenyListRemote.class, IIOPDenyListHome.class, DangerousClass.class, ClientEjb.class);
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
    public void testDefaultDenyListBlocksKnownGadgetClass() throws Exception {
        final ClientEjb ejb = getClient();
        try {
            ejb.invokeObjectMethod(new UIDefaults());
            Assert.fail("Expected exception due to default deny-list filter");
        } catch (Exception e) {
            // The exception is wrapped in MarshalException/CORBA MARSHAL, so check the entire exception chain.
            // The ORB always reports a rejection as "Deserialization of class <name> rejected by filter".
            String fullMessage = getFullExceptionMessage(e);
            String expected = "Deserialization of class " + UIDefaults.class.getName() + " rejected by filter";
            Assert.assertTrue("Exception chain should report filter rejection of UIDefaults, but was: " + fullMessage,
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
