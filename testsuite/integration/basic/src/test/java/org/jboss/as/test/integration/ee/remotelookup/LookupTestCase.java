/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.remotelookup;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertNotNull;

import java.net.SocketPermission;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
*/
@RunWith(Arquillian.class)
public class LookupTestCase {

    @ContainerResource
    private Context remoteContext;

    @Deployment(name = "test")
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "deploy.jar");
        jar.addClass(StatelessBean.class);
        jar.addAsManifestResource(createPermissionsXmlAsset(
                    new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "resolve")
                ), "permissions.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("test")
    @InSequence(1)
    public void testServerLocalLookup() throws Exception {
        InitialContext context = new InitialContext();

        lookupConnectionFactory(context, "java:jboss/exported/jms/RemoteConnectionFactory");

        context.close();
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testClientRemoteLookup() throws Exception {
        lookupConnectionFactory(remoteContext, "jms/RemoteConnectionFactory");
    }

    private void lookupConnectionFactory(Context context, String name) throws Exception {
        ConnectionFactory cf = (ConnectionFactory) context.lookup(name);
        assertNotNull(cf);
        //"java.net.SocketPermission" "localhost" "resolve"
        Connection conn = cf.createConnection("guest", "guest");
        conn.close();
    }
}
