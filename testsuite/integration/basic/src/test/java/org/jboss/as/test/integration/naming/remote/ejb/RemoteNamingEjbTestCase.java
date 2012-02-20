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

import java.net.URL;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author John Bailey, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteNamingEjbTestCase {
    private static final String ARCHIVE_NAME = "test";
    private static final Integer PORT = 4447;
    
    @ArquillianResource
    private URL baseUrl;
    
    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Remote.class, Bean.class, Singleton.class, StatefulBean.class);
        return jar;
    }

    public InitialContext getRemoteContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote://" + baseUrl.getHost() + ":" + PORT);
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

            Remote remote  = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + Bean.class.getSimpleName() + "!" + Remote.class.getName());
            assertNotNull(remote);
            assertEquals("Echo: test", remote.echo("test"));
            
            remote  = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + Singleton.class.getSimpleName() + "!" + Remote.class.getName());
            assertNotNull(remote);
            assertEquals("Echo: test", remote.echo("test"));
            
            remote  = (Remote) ctx.lookup(ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!" + Remote.class.getName());
            assertNotNull(remote);
            assertEquals("Echo: test", remote.echo("test"));

        } finally {
            ctx.close();
            Thread.currentThread().setContextClassLoader(current);
        }
    }
}
