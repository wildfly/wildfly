/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class RemoteEJBJndiBasedInvocationTestCase {

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "remote-ejb-jndi-test-case";

    private static Context context;

    @Deployment(testable = false) // the incorrectly named "testable" attribute tells Arquillian whether or not
    // it should add Arquillian specific metadata to the archive (which ultimately transforms it to a WebArchive).
    // We don't want that, so set that flag to false
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(RemoteCounter.class.getPackage());

        return jar;
    }

    @BeforeAll
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @Test
    public void testRemoteSLSBInvocation() throws Exception {
        final RemoteEcho remoteEcho = (RemoteEcho) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + EchoBean.class.getSimpleName() + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteEcho, "Lookup returned a null bean proxy");
        final String msg = "Hello world from a really remote client!!!";
        final String echo = remoteEcho.echo(msg);
        Assertions.assertEquals(msg, echo, "Unexpected echo returned from remote bean");
    }

    @Test
    public void testRemoteSFSBInvocation() throws Exception {
        final RemoteCounter remoteCounter = (RemoteCounter) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                + "/" + CounterBean.class.getSimpleName() + "!" + RemoteCounter.class.getName() + "?stateful");
        Assertions.assertNotNull(remoteCounter, "Lookup returned a null bean proxy");
        Assertions.assertEquals(0, remoteCounter.getCount(), "Unexpected initial count returned by bean");
        final int NUM_TIMES = 25;
        // test increment
        for (int i = 0; i < NUM_TIMES; i++) {
            remoteCounter.incrementCount();
            final int currentCount = remoteCounter.getCount();
            Assertions.assertEquals(i + 1, currentCount, "Unexpected count after increment");
        }
        Assertions.assertEquals(NUM_TIMES, remoteCounter.getCount(), "Unexpected total count after increment");
        // test decrement
        for (int i = NUM_TIMES; i > 0; i--) {
            remoteCounter.decrementCount();
            final int currentCount = remoteCounter.getCount();
            Assertions.assertEquals(i - 1, currentCount, "Unexpected count after decrement");
        }
        Assertions.assertEquals(0, remoteCounter.getCount(), "Unexpected total count after decrement");

    }

}
