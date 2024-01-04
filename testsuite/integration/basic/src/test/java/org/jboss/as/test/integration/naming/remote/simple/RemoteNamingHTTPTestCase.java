/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.remote.simple;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;

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
        AuthenticationConfiguration config = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
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
