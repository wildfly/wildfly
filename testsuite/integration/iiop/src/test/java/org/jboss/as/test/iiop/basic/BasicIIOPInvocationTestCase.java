/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

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
                .addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml");
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
    public void testHandle() throws IOException, NamingException {
        final ClientEjb ejb = client();
        Assert.assertEquals("hello", ejb.getRemoteViaHandleMessage());
    }

    /**
     * Tests that even if a handle is returned embedded in another object the substitution service will
     * replace it with a correct IIOP version of a handle.
     */
    @Test
    @OperateOnDeployment("client")
    public void testWrappedHandle() throws IOException, NamingException {
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
