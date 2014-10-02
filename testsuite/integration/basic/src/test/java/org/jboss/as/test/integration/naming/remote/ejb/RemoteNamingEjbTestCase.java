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

import java.io.IOException;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.naming.subsystem.NamingExtension;
import org.jboss.as.naming.subsystem.NamingSubsystemModel;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.CLASS;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.EXTERNAL_CONTEXT;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.MODULE;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
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
    private static final String EXPORT_PREFIX = "java:jboss/exported/";

    @ArquillianResource
    private URL baseUrl;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Remote.class, BinderRemote.class, Bean.class, Singleton.class, StatefulBean.class, ClusteredStatefulBean.class);
        return jar;
    }

    public InitialContext getRemoteContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, managementClient.getRemoteEjbURL().toString());
        env.put("jboss.naming.client.ejb.context", true);
        env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
        env.put("jboss.naming.client.security.callback.handler.class", CallbackHandler.class.getName());
        return new InitialContext(env);
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
            expected.add(ClusteredStatefulBean.class.getSimpleName() + "!" + Remote.class.getName());

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
    public void testCustomLookup() throws Exception {
        final InitialContext ctx = getRemoteContext();

        String alias = ARCHIVE_NAME + "/CustomStateful";

        // add custom binding
        addLookupBinding(alias, EXPORT_PREFIX + ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!" + Remote.class.getName());

        try {
            Remote bean = (Remote) ctx.lookup(alias);
            bean.echo("test");
        } finally {
            removeLookupBinding(alias);
            reload();
        }

    }

    @Test
    public void testCustomClusteredLookup() throws Exception {
        final InitialContext ctx = getRemoteContext();

        String alias = ARCHIVE_NAME + "/CustomClusteredStateful";
        // add custom binding
        addLookupBinding(alias, EXPORT_PREFIX + ARCHIVE_NAME + "/" + ClusteredStatefulBean.class.getSimpleName() + "!" + Remote.class.getName());

        try {
            Remote bean = (Remote) ctx.lookup(alias);
            bean.echo("test");
        } finally {
            removeLookupBinding(alias);
            reload();
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

    private void addLookupBinding(final String alias, final String lookup) throws IOException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        address.add(BINDING, EXPORT_PREFIX + alias);

        final ModelNode bindingAdd = new ModelNode();
        bindingAdd.get(OP).set(ADD);
        bindingAdd.get(OP_ADDR).set(address);
        bindingAdd.get(BINDING_TYPE).set(LOOKUP);
        bindingAdd.get(LOOKUP).set(EXPORT_PREFIX + ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!" + Remote.class.getName());
        ModelNode addResult = managementClient.getControllerClient().execute(bindingAdd);
        Assert.assertTrue(ModelDescriptionConstants.SUCCESS.equals(addResult.get(ModelDescriptionConstants.OUTCOME).asString()));
    }

    private void removeLookupBinding(final String alias) throws IOException {
        final ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        address.add(BINDING, EXPORT_PREFIX + alias);

        final ModelNode bindingRemove = new ModelNode();
        bindingRemove.get(OP).set(REMOVE);
        bindingRemove.get(OP_ADDR).set(address);
        ModelNode removeResult = managementClient.getControllerClient().execute(bindingRemove);
        Assert.assertTrue(ModelDescriptionConstants.SUCCESS.equals(removeResult.get(ModelDescriptionConstants.OUTCOME).asString()));
    }

    private void reload() throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        managementClient.getControllerClient().execute(operation);
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(5000);
                if (managementClient.isServerInRunningState())
                    reloaded = true;
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 10)
                    throw new Exception("Server reloading failed");
            }
        }
    }

}
