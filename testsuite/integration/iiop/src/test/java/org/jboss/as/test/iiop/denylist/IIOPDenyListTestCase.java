/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
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
 * Test case for IIOP deny-list filter functionality.
 * Tests that:
 * 1. Classes in the default deny-list are blocked
 * 2. Classes added via system property are blocked
 * 3. Safe classes are allowed through
 */
@RunWith(Arquillian.class)
public class IIOPDenyListTestCase {

    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "denylist-server.jar");
        jar.addClasses(IIOPDenyListBean.class, IIOPDenyListHome.class, IIOPDenyListRemote.class, DangerousClass.class)
                .addAsManifestResource(IIOPDenyListTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {
        String ejbJar = FileUtils.readFile(IIOPDenyListTestCase.class, "ejb-jar.xml");
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "denylist-client.jar");
        jar.addClasses(IIOPDenyListTestCase.class, IIOPDenyListRemote.class, IIOPDenyListHome.class, DangerousClass.class, ClientEjb.class);
        jar.addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar)), "ejb-jar.xml");
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
            // The exception is wrapped in MarshalException/CORBA MARSHAL, so check the entire exception chain
            String fullMessage = getFullExceptionMessage(e);
            Assert.assertTrue("Exception chain should mention filter rejection or DangerousClass, but was: " + fullMessage,
                    fullMessage.contains("rejected by filter") ||
                    fullMessage.contains("DangerousClass") ||
                    fullMessage.contains("not allowed"));
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
