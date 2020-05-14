/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.multinode.transaction.nooutbound;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.util.PropertyPermission;

import javax.ejb.EJBException;
import javax.inject.Inject;

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
 * A simple EJB Remoting transaction context propagation.
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
