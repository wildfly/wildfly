/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.transaction.nooutbound;

import static org.jboss.as.test.shared.PermissionUtils.createFilePermission;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.util.Arrays;
import java.util.PropertyPermission;

import jakarta.ejb.EJBException;
import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A simple Jakarta Enterprise Beans Remoting transaction context propagation.
 */
@RunWith(Arquillian.class)
public class TransactionContextRemoteCallTestCase {

    private static final String CLIENT_CONTAINER = "multinode-client";
    public static final String CLIENT_DEPLOYMENT = "client";
    private static final String SERVER_CONTAINER = "multinode-server";
    public static final String SERVER_DEPLOYMENT = "server";
    private static final int serverPort = 8180;


    @Inject
    private ClientStatelessBean clientBean;

    @Deployment(name = CLIENT_DEPLOYMENT, testable = true)
    @TargetsContainer(CLIENT_CONTAINER)
    public static Archive<?> clientDeployment() {
        return ShrinkWrap.create(JavaArchive.class, CLIENT_DEPLOYMENT + ".jar")
            .addClasses(ClientStatelessBean.class, ServerStatelessRemote.class, ProviderUrlData.class, TestSuiteEnvironment.class,
                    ServerMandatoryStatelessBean.class, ServerNeverStatelessBean.class,
                    TransactionContextRemoteCallTestCase.class)
            .addAsManifestResource(new StringAsset("Dependencies: org.wildfly.http-client.transaction\n"), "MANIFEST.MF")
            .addAsManifestResource(createPermissionsXmlAsset(
                    createFilePermission("delete", "jbossas.multinode.client", Arrays.asList("standalone", "data", "ejb-xa-recovery", "-")),
                    new SocketPermission(TestSuiteEnvironment.formatPossibleIpv6Address(System.getProperty("node0")) + ":" + serverPort, "connect,resolve"),
                    new PropertyPermission("node1", "read")
                ), "permissions.xml");
    }

    @Deployment(name = SERVER_DEPLOYMENT, testable = false)
    @TargetsContainer(SERVER_CONTAINER)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar")
            .addClasses(ServerMandatoryStatelessBean.class, ServerNeverStatelessBean.class, ServerStatelessRemote.class);
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void remotePlusHttpMandatoryBean() {
        ProviderUrlData providerUrl = new ProviderUrlData("remote+http", TestSuiteEnvironment.getServerAddressNode1(), serverPort);
        clientBean.call(providerUrl, ServerMandatoryStatelessBean.class.getSimpleName());
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void remotePlusHttpNeverBean() {
        try {
            ProviderUrlData providerUrl = new ProviderUrlData("remote+http", TestSuiteEnvironment.getServerAddressNode1(), serverPort);
            clientBean.call(providerUrl, ServerNeverStatelessBean.class.getSimpleName());
            Assert.fail(EJBException.class.getName() + " expected, transaction context propagated to TransactionAttributeType.NEVER");
        } catch (EJBException expected) {
        }
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void httpMandatoryBean() {
        ProviderUrlData providerUrl = new ProviderUrlData("http", TestSuiteEnvironment.getServerAddressNode1(), serverPort);
        clientBean.call(providerUrl, ServerMandatoryStatelessBean.class.getSimpleName());
    }

    @Test
    @OperateOnDeployment(CLIENT_DEPLOYMENT)
    public void httpNeverBean() {
        try {
            ProviderUrlData providerUrl = new ProviderUrlData("http", TestSuiteEnvironment.getServerAddressNode1(), serverPort);
            clientBean.call(providerUrl, ServerNeverStatelessBean.class.getSimpleName());
            Assert.fail(EJBException.class.getName() + " expected, transaction context propagated to TransactionAttributeType.NEVER");
        } catch (EJBException expected) {
        }
    }
}
