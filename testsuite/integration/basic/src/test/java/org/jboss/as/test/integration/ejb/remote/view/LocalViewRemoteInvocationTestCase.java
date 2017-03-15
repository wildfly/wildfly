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

package org.jboss.as.test.integration.ejb.remote.view;

import java.util.Hashtable;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a local view exposed by an EJB is <b>not</b> invokable remotely.
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-3939
 */
@RunWith(Arquillian.class)
public class LocalViewRemoteInvocationTestCase {

    private static final Logger logger = Logger.getLogger(LocalViewRemoteInvocationTestCase.class);

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "local-view-remote-invocation-test";

    private static Context context;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(LocalViewRemoteInvocationTestCase.class.getPackage());
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    /**
     * Test that remote invocation on a local view of a stateless bean isn't allowed
     *
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void testLocalViewInvocationRemotelyOnSLSB() throws Exception {
        final LocalEcho localEcho = (LocalEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatelessEcho.class.getSimpleName() + "!" + LocalEcho.class.getName());
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            Assert.fail("Remote invocation on a local view " + LocalEcho.class.getName() + " was expected to fail");
        } catch (EJBException nsee) {
            // expected
            logger.trace("Got the expected exception on invoking on a local view, remotely", nsee);
        }
    }

    /**
     * Test that remote invocation on a local view of a stateful bean isn't allowed
     *
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void testLocalViewInvocationRemotelyOnSFSB() throws Exception {
        final LocalEcho localEcho = (LocalEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulEcho.class.getSimpleName() + "!" + LocalEcho.class.getName() + "?stateful");
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            Assert.fail("Remote invocation on a local view " + LocalEcho.class.getName() + " was expected to fail");
        } catch (EJBException nsee) {
            // expected
            logger.trace("Got the expected exception on invoking on a local view, remotely", nsee);
        }
    }

    /**
     * Test that remote invocation on a local view of a singleton bean isn't allowed
     *
     * @throws Exception
     */
    @Test
    @RunAsClient
    public void testLocalViewInvocationRemotelyOnSingleton() throws Exception {
        final LocalEcho localEcho = (LocalEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + SingletonEcho.class.getSimpleName() + "!" + LocalEcho.class.getName());
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            Assert.fail("Remote invocation on a local view " + LocalEcho.class.getName() + " was expected to fail");
        } catch (EJBException nsee) {
            // expected
            logger.trace("Got the expected exception on invoking on a local view, remotely", nsee);
        }
    }


    /**
     * Test that an in-vm invocation on a local business interface, of a SLSB, using the ejb: namespace fails, since only
     * remote view invocations are allowed for ejb: namespace
     *
     * @throws Exception
     */
    @Test
    public void testServerInVMInvocationOnLocalViewOfSLSB() throws Exception {
        final Context jndiContext = new InitialContext();
        final LocalEcho localEcho = (LocalEcho) jndiContext.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatelessEcho.class.getSimpleName() + "!" + LocalEcho.class.getName());
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            Assert.fail("Remote invocation on a local view " + LocalEcho.class.getName() + " was expected to fail");
        } catch (IllegalStateException nsee) {
            // expected
            logger.trace("Got the expected exception on invoking on a local view, remotely", nsee);
        }
    }

    /**
     * Test that an in-vm invocation on a local business interface, of a SFSB, using the ejb: namespace fails, since only
     * remote view invocations are allowed for ejb: namespace
     *
     * @throws Exception
     */
    @Test
    public void testServerInVMInvocationOnLocalViewOfSFSB() throws Exception {
        final Context jndiContext = new InitialContext();
        final LocalEcho localEcho = (LocalEcho) jndiContext.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + StatefulEcho.class.getSimpleName() + "!" + LocalEcho.class.getName() + "?stateful");
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            Assert.fail("Remote invocation on a local view " + LocalEcho.class.getName() + " was expected to fail");
        } catch (IllegalStateException nsee) {
            // expected
            logger.trace("Got the expected exception on invoking on a local view, remotely", nsee);
        }
    }

    /**
     * Test that an in-vm invocation on a local business interface, of a singleton bean, using the ejb: namespace fails, since only
     * remote view invocations are allowed for ejb: namespace
     *
     * @throws Exception
     */
    @Test
    public void testServerInVMInvocationOnLocalViewOfSingleton() throws Exception {
        final Context jndiContext = new InitialContext();
        final LocalEcho localEcho = (LocalEcho) jndiContext.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + SingletonEcho.class.getSimpleName() + "!" + LocalEcho.class.getName());
        final String message = "Silence!";
        try {
            final String echo = localEcho.echo(message);
            Assert.fail("Remote invocation on a local view " + LocalEcho.class.getName() + " was expected to fail");
        } catch (IllegalStateException nsee) {
            // expected
            logger.trace("Got the expected exception on invoking on a local view, remotely", nsee);
        }
    }

}
