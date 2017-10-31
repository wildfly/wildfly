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

package org.jboss.as.test.integration.naming.remote.ejb;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.DefaultConfiguration;
import org.wildfly.naming.java.permission.JndiPermission;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author John Bailey, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteNamingEjbTestCase {
    private static final String ARCHIVE_NAME = "test";

    @ArquillianResource
    private URL baseUrl;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Remote.class, BinderRemote.class, Bean.class, Singleton.class, StatefulBean.class);
        jar.addAsResource(createPermissionsXmlAsset(new JndiPermission("java:jboss/exported/-", "all")), "META-INF/jboss-permissions.xml");
        return jar;
    }

    public InitialContext getRemoteContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, managementClient.getRemoteEjbURL().toString());
        env.put("jboss.naming.client.ejb.context", true);
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        return new InitialContext(DefaultConfiguration.addSecurityProperties(env));
    }

    @Test
    public void testIt() throws Exception {
        final InitialContext ctx = getRemoteContext();
        final ClassLoader current = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(Remote.class.getClassLoader());

            Remote remote = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + Bean.class.getSimpleName() + "!" + Remote.class.getName());
            assertNotNull(remote);
            assertEquals("Echo: test", remote.echo("test"));

            remote = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + Singleton.class.getSimpleName() + "!" + BinderRemote.class.getName());
            assertNotNull(remote);
            assertEquals("Echo: test", remote.echo("test"));

            remote = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!" + Remote.class.getName());
            assertNotNull(remote);
            assertEquals("Echo: test", remote.echo("test"));

            final Set<String> expected = new HashSet<String>();
            expected.add(Bean.class.getSimpleName() + "!" + Remote.class.getName());
            expected.add(Singleton.class.getSimpleName() + "!" + BinderRemote.class.getName());
            expected.add(StatefulBean.class.getSimpleName() + "!" + Remote.class.getName());

            NamingEnumeration<NameClassPair> e = ctx.list("test");
            while (e.hasMore()) {
                NameClassPair binding = e.next();
                if (!expected.remove(binding.getName())) {
                    Assert.fail("unknown binding " + binding.getName());
                }
            }
            if (!expected.isEmpty()) {
                Assert.fail("bindings not found " + expected);
            }

        } finally {
            ctx.close();
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @Test
    public void testDeploymentBinding() throws Exception {
        final InitialContext ctx = getRemoteContext();
        BinderRemote binder = null;
        try {

            try {
                ctx.lookup("some/entry");
                fail("expected exception");
            } catch (NameNotFoundException e) {
                // expected
            }

            // test binding
            binder = (BinderRemote) ctx.lookup(ARCHIVE_NAME + "/" + Singleton.class.getSimpleName() + "!" + BinderRemote.class.getName());
            assertNotNull(binder);

            binder.bind();

            assertEquals("Test", ctx.lookup("some/entry"));

            NamingEnumeration<Binding> bindings = ctx.listBindings("some");
            assertTrue(bindings.hasMore());
            assertEquals("Test", bindings.next().getObject());
            assertFalse(bindings.hasMore());


            // test rebinding
            binder.rebind();

            assertEquals("Test2", ctx.lookup("some/entry"));

            bindings = ctx.listBindings("some");
            assertTrue(bindings.hasMore());
            assertEquals("Test2", bindings.next().getObject());
            assertFalse(bindings.hasMore());


            // test unbinding
            binder.unbind();

            try {
                ctx.lookup("some/entry");
                fail("expected exception");
            } catch (NameNotFoundException e) {
                // expected
            }


            // test rebinding when it doesn't already exist
            binder.rebind();

            assertEquals("Test2", ctx.lookup("some/entry"));

            bindings = ctx.listBindings("some");
            assertTrue(bindings.hasMore());
            assertEquals("Test2", bindings.next().getObject());
            assertFalse(bindings.hasMore());
        } finally {
            // clean up in case any JNDI bindings were left around
            try {
                if (binder != null)
                    binder.unbind();
            } catch (Exception e) {
                // expected
            }
            ctx.close();
        }
    }

    @Test
    public void testRemoteNamingGracefulShutdown() throws Exception {

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("suspend");
        managementClient.getControllerClient().execute(op);

        Thread.currentThread().setContextClassLoader(Remote.class.getClassLoader());

        final InitialContext ctx = getRemoteContext();
        final ClassLoader current = Thread.currentThread().getContextClassLoader();
        try {


            try {

                Remote remote = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + Bean.class.getSimpleName() + "!" + Remote.class.getName());
                Assert.fail();
            } catch (NamingException expected) {
            }

            try {
                Remote remote = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + Singleton.class.getSimpleName() + "!" + BinderRemote.class.getName());
                Assert.fail();
            } catch (NamingException expected) {
            }

            try {
                Remote remote = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!" + Remote.class.getName());
                Assert.fail();
            } catch (NamingException expected) {
            }
        } finally {
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);

            ctx.close();
            Thread.currentThread().setContextClassLoader(current);
        }
    }
}
