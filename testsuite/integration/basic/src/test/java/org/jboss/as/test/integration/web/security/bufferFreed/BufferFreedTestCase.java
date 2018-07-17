/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.web.security.bufferFreed;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketbox.util.KeyStoreUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.net.URL;
import java.util.logging.LogRecord;

import static org.junit.Assert.fail;


/**
 * Automated test for [ JBEAP-14792 ]
 * Test enables SSL on remoting/http connector
 * and runs multiple client threads that use remoting API to connect/disconnect Wildfly repeatedly.
 * If the XNIO000017 error occurs test fails.
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@ServerSetup(BufferFreedServerSetupTask.class)
@RunAsClient
public class BufferFreedTestCase {

    private static final String DEPLOYMENT = "deployment";
    private static final int THREAD_COUNT = 100;
    private static final int THREAD_SLEEP = 200;

    private static final Logger log = Logger.getLogger(BufferFreedServerSetupTask.class.getSimpleName());
    private static java.util.logging.Logger logger = LogManager.getLogManager().getLogger("org.xnio.listener");
    private static MyHandler handler = new MyHandler();
    private Config config = new Config();

    private static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

    @BeforeClass
    public static void before() {
        handler.setLevel(Level.INFO);
        logger.addHandler(handler);
    }

    @Deployment(name = DEPLOYMENT)
    public static WebArchive appDeployment1() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        return war;
    }

    @Test
    public void testRepeatedlyConnectFromMultipleThreads() throws Exception {
        MyHandler.setCallback(new CallbackHandler(this));

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(KeyStoreUtil.getKeyStore(getTrustStoreFile(), BufferFreedServerSetupTask.KEYSTORE_PASSWORD.toCharArray()),
                BufferFreedServerSetupTask.KEYSTORE_PASSWORD.toCharArray());

        // trust store factory setup
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(KeyStoreUtil.getKeyStore(getTrustStoreFile(), BufferFreedServerSetupTask.KEYSTORE_PASSWORD.toCharArray()));

        // create ssl context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        SSLContext.setDefault(sslContext);

        TestRemotingConnection[] runnables = new TestRemotingConnection[THREAD_COUNT];
        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            if (!this.config.isRunning()) {
                fail("\"XNIO000017: Buffer was already freed\" or another ERROR occured in the log. Failing the test.");
            }
            System.out.println("Creating new instance of TestRemotingConnection nr." + i);
            String name = "TestRemotingConnection-" + i;
            runnables[i] = new TestRemotingConnection(name, config);
            threads[i] = new Thread(runnables[i], name);
            threads[i].start();
            sleep(THREAD_SLEEP);
        }

        for (int i = 0; i < THREAD_COUNT; i++) {
            try {
                if (!this.config.isRunning()) {
                    fail("\"XNIO000017: Buffer was already freed\" or another ERROR occured in the log. Failing the test.");
                }
                threads[i].join();
                if (runnables[i].getException() != null) {
                    runnables[i].getException().printStackTrace();
                }
            } catch(InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public static File getTrustStoreFile() {
        final URL scannerTestsClassFileUrl = originalClassLoader.getResource(
                BufferFreedTestCase.class.getName().replace('.', '/') + ".class"
        );
        final int position = scannerTestsClassFileUrl.getFile().lastIndexOf("/basic/");
        if (position == -1) {
            fail("Unable to get test source directory");
        }
        final String moduleDirectoryPath = scannerTestsClassFileUrl.getFile().substring(0, position + "/basic".length());
        final File keystoreFile = new File(moduleDirectoryPath, "src/test/resources/identity.jks");
        return keystoreFile;
    }

    private void stopRunning() {
        this.config.setRunning(false);
    }

    private static void sleep(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch(Exception e) {
            log.error(e);
        }
    }

    @AfterClass
    public static void after() {
        logger.removeHandler(handler);
    }

    private static class CallbackHandler implements MyHandler.Callback {
        BufferFreedTestCase parent;

        CallbackHandler(BufferFreedTestCase parent) {
            this.parent = parent;
        }

        @Override
        public void warn(LogRecord logRecord) {
            log.trace(String.format("%s %s\n", logRecord.getLevel(), logRecord.getMessage()));
        }

        @Override
        public void error(LogRecord logRecord) {
            // stop the test when the corrent error code was found
            if (logRecord.getLevel().equals(java.util.logging.Level.SEVERE) || logRecord.getMessage().contains("XNIO001007")) {
                parent.stopRunning();
            }

            log.trace(String.format("%s %s\n", logRecord.getLevel(), logRecord.getMessage()));
        }
    }
}