/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.basic;

import java.io.IOException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.as.test.shared.PropertiesValueResolver;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * A simple IIOP invocation for one AS7 server to another
 */
@RunWith(Arquillian.class)
public class BasicIIOPInvocationTestCase {


    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "server.jar");
        jar.addClasses(IIOPBasicBean.class, IIOPBasicHome.class, IIOPBasicRemote.class,
                IIOPBasicStatefulBean.class, IIOPBasicStatefulHome.class, IIOPBasicStatefulRemote.class,
                HandleWrapper.class)
                .addAsManifestResource(BasicIIOPInvocationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
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
        String ejbJar = FileUtils.readFile(BasicIIOPInvocationTestCase.class, "ejb-jar.xml");

        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if(properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        jar.addClasses(ClientEjb.class, IIOPBasicHome.class, IIOPBasicRemote.class,
                BasicIIOPInvocationTestCase.class, IIOPBasicStatefulHome.class,
                IIOPBasicStatefulRemote.class, HandleWrapper.class)
                .addAsManifestResource(BasicIIOPInvocationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml")
                .addAsManifestResource(
                        PermissionUtils.createPermissionsXmlAsset(new ElytronPermission("getPrivateCredentials")),
                        "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testRemoteIIOPInvocation() throws IOException, NamingException {
        final ClientEjb ejb = client();
        Assert.assertEquals("hello", ejb.getRemoteMessage());
    }

    @Test
    @OperateOnDeployment("client")
    public void testHomeHandle() throws IOException, NamingException {
        final ClientEjb ejb = client();
        Assert.assertEquals("hello", ejb.getRemoteViaHomeHandleMessage());
    }

    @Test
    @OperateOnDeployment("client")
    public void testHandle() throws IOException, NamingException, ClassNotFoundException {
        final ClientEjb ejb = client();
        Assert.assertEquals("hello", ejb.getRemoteViaHandleMessage());
    }

    /**
     * Tests that even if a handle is returned embedded in another object the substitution service will
     * replace it with a correct IIOP version of a handle.
     */
    @Test
    @OperateOnDeployment("client")
    public void testWrappedHandle() throws IOException, NamingException, ClassNotFoundException {
        final ClientEjb ejb = client();
        Assert.assertEquals("hello", ejb.getRemoteViaWrappedHandle());
    }

    @Test
    @OperateOnDeployment("client")
    public void testEjbMetadata() throws IOException, NamingException {
        final ClientEjb ejb = client();
        Assert.assertEquals("hello", ejb.getRemoteMessageViaEjbMetadata());
    }


    @Test
    @OperateOnDeployment("client")
    public void testIsIdentical() throws IOException, NamingException {
        final ClientEjb ejb = client();
        ejb.testIsIdentical();
    }

    private ClientEjb client() throws NamingException {
        final InitialContext context = new InitialContext();
        return (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
    }
}
