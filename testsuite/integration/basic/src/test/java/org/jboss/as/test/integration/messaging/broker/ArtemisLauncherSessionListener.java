/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.broker;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.core.settings.impl.AddressFullMessagePolicy;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.utils.FileUtil;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Starts an embedded Artemis broker, to be used as a remote broker by the server under test, for the duration of a JUnit Platform launcher session (a surefire execution).
 * Registered via {@link java.util.ServiceLoader}, it only starts the broker if the {@code artemis.home} system property, which configures the broker data directory, is set.
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public class ArtemisLauncherSessionListener implements LauncherSessionListener {

    private ActiveMQServer server;

    // Using files may be useful for debugging (through print-data for instance)
    private static final boolean PERSISTENCE = false;

    public static void main(final String[] args) throws Exception {
        ArtemisLauncherSessionListener listener = new ArtemisLauncherSessionListener();
        try {
            listener.startServer();

            System.out.println("Server started, ready to start client test");

            // create the reader before printing OK so that if the test is quick
            // we will still capture the STOP message sent by the client
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isr);

            System.out.println("OK");

            String line = br.readLine();
            if (line != null && "STOP".equals(line.trim())) {
                listener.stopServer();
                System.out.println("Server stopped");
                System.exit(0);
            } else {
                // stop anyway but with an error status
                System.exit(1);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            String allStack = t.getCause().getMessage() + "|";
            StackTraceElement[] stackTrace = t.getCause().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                allStack += stackTraceElement.toString() + "|";
            }
            System.out.println(allStack);
            System.out.println("KO");
            System.exit(1);
        }
    }

    private synchronized void startServer() throws Exception {
        if (server == null) {
            Configuration config = new ConfigurationImpl()
                    .setName("Test-Broker")
                    .addAcceptorConfiguration("netty", getBrokerUrl())
                    .setSecurityEnabled(false)
                    .setPersistenceEnabled(false)
                    .setJournalFileSize(10 * 1024 * 1024)
                    .setJournalDeviceBlockSize(4096)
                    .setJournalMinFiles(2)
                    .setJournalDatasync(true)
                    .setJournalPoolFiles(10)
                    .addConnectorConfiguration("netty", getBrokerUrl());
            File dataPlace = new File(getArtemisHome());

            FileUtil.deleteDirectory(dataPlace);

            config.setJournalDirectory(new File(dataPlace, "./journal").getAbsolutePath()).
                    setPagingDirectory(new File(dataPlace, "./paging").getAbsolutePath()).
                    setLargeMessagesDirectory(new File(dataPlace, "./largemessages").getAbsolutePath()).
                    setBindingsDirectory(new File(dataPlace, "./bindings").getAbsolutePath()).setPersistenceEnabled(true);
            server = ActiveMQServers.newActiveMQServer(config, PERSISTENCE);
            server.getAddressSettingsRepository().addMatch("#", new AddressSettings()
                    .setDeadLetterAddress(SimpleString.of("DLQ"))
                    .setExpiryAddress(SimpleString.of("ExpiryQueue"))
                    .setRedeliveryDelay(0)
                    .setMaxSizeBytes(-1)
                    .setMessageCounterHistoryDayLimit(10)
                    .setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE)
                    .setAutoCreateQueues(true)
                    .setAutoCreateAddresses(true)
                    .setAutoCreateJmsQueues(true)
                    .setAutoCreateJmsTopics(true));
            server.getAddressSettingsRepository().addMatch("activemq.management#", new AddressSettings()
                    .setDeadLetterAddress(SimpleString.of("DLQ"))
                    .setExpiryAddress(SimpleString.of("ExpiryQueue"))
                    .setRedeliveryDelay(0)
                    .setMaxSizeBytes(-1)
                    .setMessageCounterHistoryDayLimit(10)
                    .setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE)
                    .setAutoCreateQueues(true)
                    .setAutoCreateAddresses(true)
                    .setAutoCreateJmsQueues(true)
                    .setAutoCreateJmsTopics(true));
            server.start();
            server.waitForActivation(10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        if (System.getProperty("artemis.home") != null) {
            try {
                startServer();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to start the Artemis broker", e);
            }
        }
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        try {
            stopServer();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stop the Artemis broker", e);
        }
    }

    private String getArtemisHome() {
        String artemisHome = System.getProperty("artemis.dist");
        if (artemisHome == null) {
            artemisHome = System.getProperty("artemis.home");
        }
        return artemisHome == null ? System.getenv("ARTEMIS_HOME") : artemisHome;
    }

    private synchronized void stopServer() throws Exception {
        if (server != null) {
            server.stop();
        }
        server = null;
    }

    private String getBrokerUrl() {
        return "tcp://" + getServerAddress() + ":" + getServerPort();
    }

    private String getServerAddress() {
        String address = System.getProperty("management.address");
        if (address == null) {
            address = System.getProperty("node0");
        }
        if (address != null) {
            return formatPossibleIpv6Address(address);
        }
        return "localhost";
    }

    private String formatPossibleIpv6Address(String address) {
        if (address == null) {
            return address;
        }
        if (!address.contains(":")) {
            return address;
        }
        if (address.startsWith("[") && address.endsWith("]")) {
            return address;
        }
        return "[" + address + "]";
    }

    private int getServerPort() {
        //this here is just fallback logic for older testsuite code that wasn't updated to newer property names
        return Integer.getInteger("artemis.port", 61616);
    }
}
