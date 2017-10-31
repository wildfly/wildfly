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

package org.jboss.as.test.iiop.transaction;

import java.io.IOException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

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
public class TransactionIIOPInvocationTestCase {

    @Deployment(name = "server", testable = false)
    @TargetsContainer("iiop-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "server.jar");
        jar.addClasses(IIOPTransactionalStatelessBean.class, IIOPTransactionalHome.class,
                IIOPTransactionalRemote.class, IIOPTransactionalStatefulHome.class,
                IIOPTransactionalStatefulRemote.class, IIOPTransactionalStatefulBean.class)
            .addAsManifestResource(TransactionIIOPInvocationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("iiop-client")
    public static Archive<?> clientDeployment() {

        String ejbJar = FileUtils.readFile(TransactionIIOPInvocationTestCase.class, "ejb-jar.xml");

        final Properties properties = new Properties();
        properties.putAll(System.getProperties());
        if(properties.containsKey("node1")) {
            properties.put("node1", NetworkUtils.formatPossibleIpv6Address((String) properties.get("node1")));
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        jar.addClasses(ClientEjb.class, IIOPTransactionalHome.class,
                IIOPTransactionalRemote.class, TransactionIIOPInvocationTestCase.class
                , IIOPTransactionalStatefulHome.class, IIOPTransactionalStatefulRemote.class)
                .addAsManifestResource(TransactionIIOPInvocationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset(PropertiesValueResolver.replaceProperties(ejbJar, properties)), "ejb-jar.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testRemoteIIOPInvocation() throws IOException, NamingException, NotSupportedException, SystemException {
        final InitialContext context = new InitialContext();
        final ClientEjb ejb = (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
        ejb.basicTransactionPropagationTest();

    }

    @Test
    @OperateOnDeployment("client")
    public void testRollbackOnly() throws IOException, NamingException, NotSupportedException, SystemException {
        final InitialContext context = new InitialContext();
        final ClientEjb ejb = (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
        ejb.testRollbackOnly();
    }

    @Test
    @OperateOnDeployment("client")
    public void testRollbackOnlyBeforeCompletion() throws IOException, NamingException, NotSupportedException, SystemException, HeuristicMixedException, HeuristicRollbackException {
        final InitialContext context = new InitialContext();
        final ClientEjb ejb = (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
        ejb.testRollbackOnlyBeforeCompletion();
    }

    @Test
    @OperateOnDeployment("client")
    public void testSameTransactionEachCall() throws IOException, NamingException, NotSupportedException, SystemException {
        final InitialContext context = new InitialContext();
        final ClientEjb ejb = (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
        ejb.testSameTransactionEachCall();
    }


    @Test
    @OperateOnDeployment("client")
    public void testSynchronizationSucceeded() throws IOException, NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        final InitialContext context = new InitialContext();
        final ClientEjb ejb = (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
        ejb.testSynchronization(true);
    }


    @Test
    @OperateOnDeployment("client")
    public void testSynchronizationFailed() throws IOException, NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        final InitialContext context = new InitialContext();
        final ClientEjb ejb = (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
        ejb.testSynchronization(false);
    }
}
