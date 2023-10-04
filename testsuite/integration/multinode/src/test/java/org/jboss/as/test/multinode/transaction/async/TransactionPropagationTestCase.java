/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.transaction.async;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.Arrays;

import javax.naming.InitialContext;

/**
 * <p>
 * Testing transaction propagation to a remote server when bean method is annotated
 * as asynchronous.
 * <p>
 * Specification says at such case that no transaction context is provided
 * <p>
 * Jakarta Enterprise Beans 3.2 4.5.3 Transactions<br>
 * The client’s transaction context does not propagate with an asynchronous method invocation. From the
 * Bean Provider’s point of view, there is never a transaction context flowing in from the client. This
 * means, for example, that the semantics of the REQUIRED transaction attribute on an asynchronous
 * method are exactly the same as REQUIRES_NEW.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class TransactionPropagationTestCase {

    public static final String CLIENT_DEPLOYMENT = "client-txt-propag-async";
    public static final String SERVER_DEPLOYMENT = "server-txt-propag-async";

    @Deployment(name = CLIENT_DEPLOYMENT, testable = true)
    @TargetsContainer("multinode-client")
    public static Archive<?> clientDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, CLIENT_DEPLOYMENT + ".jar");
        jar.addPackage(TransactionPropagationTestCase.class.getPackage());
        jar.addAsManifestResource("META-INF/jboss-ejb-client-receivers.xml", "jboss-ejb-client.xml");
        jar.addAsManifestResource(
                createPermissionsXmlAsset(
                        createFilePermission("read",
                                "jboss.home", Arrays.asList("standalone", "tmp", "auth", "-"))),
                "permissions.xml");
        return jar;
    }

    @Deployment(name = SERVER_DEPLOYMENT, testable = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar");
        jar.addClasses(TransactionalMandatory.class, TransactionalStatusByManager.class, TransactionalStatusByRegistry.class,
            TransactionalRemote.class);
        return jar;
    }


    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void testRemoteInvocation() throws Exception {
        final ClientBean ejb = getClient();
        ejb.callToMandatory();
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void testRemoteWithStatusAtRegistry() throws Exception {
        final ClientBean ejb = getClient();
        ejb.callToStatusByRegistry();
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void testRemoteWithStatusAtTransactionManager() throws Exception {
        final ClientBean ejb = getClient();
        ejb.callToStatusByTransactionmanager();
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void testRemoteWithRequired() throws Exception {
        final ClientBean ejb = getClient();
        ejb.callToRequired();
    }

    private ClientBean getClient() throws Exception {
        final InitialContext context = new InitialContext();
        final ClientBean lookupResult = (ClientBean) context.lookup("java:module/" + ClientBean.class.getSimpleName());
        context.close();
        return lookupResult;
    }
}
