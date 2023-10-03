/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.transaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import java.io.IOException;
import java.util.Arrays;

import static org.jboss.as.test.shared.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

/**
 * A simple Jakarta Enterprise Beans Remoting transaction context propagation in JTS style from one AS7 server to another.
 *
 * @author Stuart Douglas
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
public class TransactionInvocationTestCase {

    public static final String SERVER_DEPLOYMENT = "server";
    public static final String CLIENT_DEPLOYMENT = "client";

    @Deployment(name = "server", testable = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar");
        jar.addClasses(TransactionalStatelessBean.class, TransactionalRemote.class,
                TransactionalStatefulRemote.class, TransactionalStatefulBean.class);
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("multinode-client")
    public static Archive<?> clientDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, CLIENT_DEPLOYMENT + ".jar");
        jar.addClasses(ClientEjb.class, TransactionalRemote.class, TransactionInvocationTestCase.class,
                TransactionalStatefulRemote.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        createFilePermission("delete",
                                "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-")),
                        createFilePermission("read",
                                "jboss.home", Arrays.asList("standalone", "tmp", "auth", "-"))),
                "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testRemoteInvocation() throws IOException, NamingException, NotSupportedException, SystemException {
        final ClientEjb ejb = getClient();
        ejb.basicTransactionPropagationTest();

    }

    @Test
    @OperateOnDeployment("client")
    public void testRollbackOnly() throws IOException, NamingException, NotSupportedException, SystemException {
        final ClientEjb ejb = getClient();
        ejb.testRollbackOnly();
    }

    @Test
    @OperateOnDeployment("client")
    public void testRollbackOnlyBeforeCompletion() throws IOException, NamingException, NotSupportedException, SystemException, HeuristicMixedException, HeuristicRollbackException {
        final ClientEjb ejb = getClient();
        ejb.testRollbackOnlyBeforeCompletion();
    }

    @Test
    @OperateOnDeployment("client")
    public void testSameTransactionEachCall() throws IOException, NamingException, NotSupportedException, SystemException {
        final ClientEjb ejb = getClient();
        ejb.testSameTransactionEachCall();
    }


    @Test
    @OperateOnDeployment("client")
    public void testSynchronizationSucceeded() throws IOException, NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        final ClientEjb ejb = getClient();
        ejb.testSynchronization(true);
    }


    @Test
    @OperateOnDeployment("client")
    public void testSynchronizationFailed() throws IOException, NamingException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        final ClientEjb ejb = getClient();
        ejb.testSynchronization(false);
    }

    private ClientEjb getClient() throws NamingException {
        final InitialContext context = new InitialContext();
        return (ClientEjb) context.lookup("java:module/" + ClientEjb.class.getSimpleName());
    }
}
