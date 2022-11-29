/*
 * Copyright 2020 Emmanuel Hugonnet (c) 2020 Red Hat, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.common.jms;

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
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public class ArtemisRunListener extends RunListener {

    private ActiveMQServer server;

    // Using files may be useful for debugging (through print-data for instance)
    private static final boolean PERSISTENCE = false;

    public static void main(final String[] args) throws Exception {
        ArtemisRunListener listener = new ArtemisRunListener();
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

    private synchronized ActiveMQServer startServer() throws Exception {
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
                    .setDeadLetterAddress(SimpleString.toSimpleString("DLQ"))
                    .setExpiryAddress(SimpleString.toSimpleString("ExpiryQueue"))
                    .setRedeliveryDelay(0)
                    .setMaxSizeBytes(-1)
                    .setMessageCounterHistoryDayLimit(10)
                    .setAddressFullMessagePolicy(AddressFullMessagePolicy.PAGE)
                    .setAutoCreateQueues(true)
                    .setAutoCreateAddresses(true)
                    .setAutoCreateJmsQueues(true)
                    .setAutoCreateJmsTopics(true));
            server.getAddressSettingsRepository().addMatch("activemq.management#", new AddressSettings()
                    .setDeadLetterAddress(SimpleString.toSimpleString("DLQ"))
                    .setExpiryAddress(SimpleString.toSimpleString("ExpiryQueue"))
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
        return server;
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        stopServer();
        super.testRunFinished(result);
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        startServer();
        super.testRunStarted(description);
    }

    @Override
    public void testStarted(Description description) throws Exception {
        super.testStarted(description);
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
