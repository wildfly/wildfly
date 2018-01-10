/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case based on reproducer for <a href="https://issues.jboss.org/browse/JBPAPP-7897">JBPAPP-7897</a>
 *
 * Test case checks propagation of role from {@code RunAs} when ejb2, ejb3, mdb beans are part of the chain.
 *
 * @author Derek Horton, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RunAsEjbMdbTestCase.RunAsTestCaseEJBMDBSetup.class)
public class RunAsEjbMdbTestCase {

    private static final String PRINCIPAL = "anonymous";

    @ContainerResource
    private InitialContext initialContext;

    static class RunAsTestCaseEJBMDBSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.createJmsQueue(HelloBean.QUEUE_NAME, HelloBean.QUEUE_NAME_JNDI);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.removeJmsQueue(HelloBean.QUEUE_NAME);
        }
    }

    @Deployment(testable = false, managed = true, name = "ejb2", order = 1)
    public static Archive<?> runAsEJB2() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "runasmdbejb-ejb2.jar")
            .addClasses(GoodBye.class, GoodByeBean.class, GoodByeHome.class, GoodByeLocal.class, GoodByeLocalHome.class);
        jar.addAsManifestResource(RunAsEjbMdbTestCase.class.getPackage(), "ejb-jar-ejb2.xml", "ejb-jar.xml");
        return jar;
    }

    @Deployment(testable = false, managed = true, name = "ejb3", order = 2)
    public static Archive<?> runAsEJB3() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "runasmdbejb-ejb3.jar")
            .addClasses(HelloBean.class,  Hello.class, HolaBean.class, Hola.class,
                Howdy.class, HowdyBean.class, HelloMDB.class, TimeoutUtil.class);
        jar.addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment.runasmdbejb-ejb2.jar  \n"), "MANIFEST.MF");
        return jar;
    }

    /**
     * The setup of testcase is:
     *
     * <pre>
     * ejb client
     * +-> ejb3 slsb -> ejb3 mdb (@RunAs) -> ( ejb3 slsb -> ejb2 slsb && ejb2 slsb )
     * </pre>
     */
    @Test
    public void clientCall() throws Exception {
        Hello helloBean = (Hello) initialContext.lookup("runasmdbejb-ejb3/Hello!org.jboss.as.test.integration.ejb.security.runas.ejb2mdb.Hello");
        String hellomsg = helloBean.sayHello();
        Assert.assertEquals(String.format("%s %s, %s %s, %s %s! %s.",
            HelloBean.SAYING, PRINCIPAL, HowdyBean.SAYING, PRINCIPAL, HolaBean.SAYING, PRINCIPAL, GoodByeBean.SAYING), hellomsg);
    }

    /**
     * The setup of testcase is:
     *
     * <pre>
     * send message
     * +-> ejb3 mdb (@RunAs) -> ( ejb3 slsb -> ejb2 slsb && ejb2 slsb )
     * </pre>
     */
    @Test
    public void sendMessage() throws Exception {
        ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("jms/RemoteConnectionFactory");
        Queue queue = (Queue) initialContext.lookup(HelloBean.QUEUE_NAME);
        String replyMessage =  HelloBean.sendMessage(cf, queue);

        Assert.assertEquals(String.format("%s %s, %s %s! %s.",
            HowdyBean.SAYING, PRINCIPAL, HolaBean.SAYING, PRINCIPAL, GoodByeBean.SAYING), replyMessage);
    }
}
