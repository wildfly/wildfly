/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.security;

import java.rmi.RemoteException;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.PropertiesValueResolver;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * A simple IIOP invocation for one AS7 server to another
 */
@RunWith(Arquillian.class)
public class IIOPSecurityInvocationTestCase {


    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "server.jar");
        jar.addClasses(IIOPSecurityStatelessBean.class, IIOPSecurityStatelessHome.class, IIOPSecurityStatelessRemote.class)
                .addAsManifestResource(IIOPSecurityInvocationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {

        /*
         * The @EJB annotation doesn't allow to specify the address dynamically. So, istead of
         *       @EJB(lookup = "corbaname:iiop:localhost:3628#IIOPTransactionalStatelessBean")
         *       private IIOPTransactionalHome home;
         * we need to do this trick to get the ${node0} sys prop into ejb-jar.xml
         * and have it injected that way.
         */
        String ejbJar = FileUtils.readFile(IIOPSecurityInvocationTestCase.class, "ejb-jar.xml");

        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if (properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        jar.addClasses(ClientEjb.class, IIOPSecurityStatelessHome.class, IIOPSecurityStatelessRemote.class, IIOPSecurityInvocationTestCase.class, Util.class)
                .addAsManifestResource(IIOPSecurityInvocationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml")
                // the following permission is needed because of usage of LoginContext in the test
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getSecurityDomain"), new ElytronPermission("getPrivateCredentials")), "permissions.xml");

        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testSuccessfulInvocation() throws Exception {
        Callable<Void> callable = () -> {
            final ClientEjb ejb = client();
            Assert.assertEquals("role1", ejb.testSuccess());
            return null;
        };
        Util.switchIdentity("user1", "password1", callable);
    }

    @Test
    @OperateOnDeployment("client")
    public void testFailedInvocation() throws Exception {
        Callable<Void> callable = () -> {
            final ClientEjb ejb = client();
            ejb.testFailure();
            return null;
        };
        try {
            Util.switchIdentity("user1", "password1", callable);
            Assert.fail("Invocation should have failed");
        } catch (RemoteException expected) {
        }
    }

    private ClientEjb client() throws NamingException {
        final InitialContext context = new InitialContext();
        return (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
    }
}
