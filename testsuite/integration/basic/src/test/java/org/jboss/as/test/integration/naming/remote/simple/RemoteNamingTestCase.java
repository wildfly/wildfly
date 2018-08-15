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

package org.jboss.as.test.integration.naming.remote.simple;

import java.net.URI;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.wildfly.naming.java.permission.JndiPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

/**
 * @author John Bailey
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteNamingTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(BindingEjb.class)
                // BindingEjb binds java:jboss/exported/test and java:jboss/exported/context/test
                .addAsManifestResource(createPermissionsXmlAsset(
                        new JndiPermission("java:jboss/exported/test", "bind"),
                        new JndiPermission("java:jboss/exported/context/test", "bind")),
                        "permissions.xml");

        return jar;
    }

    public  Context getRemoteContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        URI webUri = managementClient.getWebUri();
        //TODO replace with remote+http once the New WildFly Naming Client is merged
        URI namingUri = new URI("http-remoting", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "", "" ,"");
        env.put(Context.PROVIDER_URL, namingUri.toString());
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        return new InitialContext(DefaultConfiguration.addSecurityProperties(env));
    }

    @Test
    public void testRemoteLookup() throws Exception {
        Context context = null;
        try {
            context = getRemoteContext();
            assertEquals("TestValue", context.lookup("test"));
        } finally {
            if (context != null)
                context.close();
        }
    }

    @Test
    public void testRemoteContextLookup() throws Exception {
        Context context = null;
        try {
            context = getRemoteContext();
            assertEquals("TestValue", ((Context) context.lookup("")).lookup("test"));
        } finally {
            if (context != null)
                context.close();
        }
    }

    @Test
    public void testNestedLookup() throws Exception {
        Context context = null;
        try {
            context = getRemoteContext();
            assertEquals("TestValue", ((Context) context.lookup("context")).lookup("test"));
        } finally {
            if (context != null)
                context.close();
        }
    }
}
