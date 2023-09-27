/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
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
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
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
