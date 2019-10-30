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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.naming.java.permission.JndiPermission;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.net.URI;
import java.util.Properties;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteNamingHTTPTestCase {

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

    public  Context getRemoteHTTPContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        URI webUri = managementClient.getWebUri();
        URI namingUri = new URI("http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "/wildfly-services", "" ,"");
        env.put(Context.PROVIDER_URL, namingUri.toString());
        env.put(Context.SECURITY_PRINCIPAL, System.getProperty("jboss.application.username", "guest"));
        env.put(Context.SECURITY_CREDENTIALS, System.getProperty("jboss.application.username", "guest"));
        return new InitialContext(env);
    }

    private static AuthenticationContext old;
    @BeforeClass
    public static void setup() {
        AuthenticationConfiguration config = AuthenticationConfiguration.EMPTY.useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, config);
        old = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(context);
    }

    @AfterClass
    public static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(old);
    }

    @Test
    public void testHTTPRemoteLookup() throws Exception {
        Context context = null;
        try {
            context = getRemoteHTTPContext();
            assertEquals("TestValue", context.lookup("test"));
        } finally {
            if (context != null)
                context.close();
        }
    }

    @Test
    public void testHTTPRemoteContextLookup() throws Exception {
        Context context = null;
        try {
            context = getRemoteHTTPContext();
            assertEquals("TestValue", ((Context) context.lookup("")).lookup("test"));
        } finally {
            if (context != null)
                context.close();
        }
    }

    @Test
    public void testHTTPNestedLookup() throws Exception {
        Context context = null;
        try {
            context = getRemoteHTTPContext();
            assertEquals("TestValue", ((Context) context.lookup("context")).lookup("test"));
        } finally {
            if (context != null)
                context.close();
        }
    }
}
