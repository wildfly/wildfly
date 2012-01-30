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

import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author John Bailey
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteNamingTestCase {
    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar");
        jar.addClasses(BindingActivator.class);
        jar.addAsManifestResource("naming/remote/simple/services", "services");
        jar.addAsManifestResource("naming/remote/simple/MANIFEST.MF", "MANIFEST.MF");
        return jar;
    }

    private static Context remoteContext;
    
    @BeforeClass
    public static void setupRemoteContext() throws Exception {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
        env.put(Context.PROVIDER_URL, "remote://localhost:4447");
        remoteContext = new InitialContext(env);
    }

    @Test
    public void testRemoteLookup() throws Exception {
        assertEquals("TestValue", remoteContext.lookup("test"));
    }

    @Test
    public void testRemoteContextLookup() throws Exception {
        assertEquals("TestValue", ((Context) remoteContext.lookup("")).lookup("test"));
    }

    @Test
    public void testNestedLookup() throws Exception {
        assertEquals("TestValue", ((Context)remoteContext.lookup("context")).lookup("test"));
    }
}
