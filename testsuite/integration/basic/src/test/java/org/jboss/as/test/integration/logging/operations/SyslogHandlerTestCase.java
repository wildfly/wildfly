/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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


package org.jboss.as.test.integration.logging.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.event.printstream.FileSyslogServerEventHandler;

/**
 * A SyslogHandlerTestCase for testing that logs are logged to syslog
 *
 * @author Ondrej Lukas
 */
@RunWith(Arquillian.class)
@ServerSetup(SyslogHandlerTestCase.SyslogHandlerTestCaseSetup.class)
@Ignore("WFLY-1584 - Events may not be getting fired before the file read is done.")
public class SyslogHandlerTestCase {

    private static final Logger LOGGER = Logger.getLogger(SyslogHandlerTestCase.class.getPackage().getName());
    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);
    private static final String PACKAGE = SyslogHandlerTestCase.class.getPackage().getName();
    private static final String FILE_NAME = "tempSyslogFile.log";
    private static final String TRACE_LOG = "trace_log_to_syslog";
    private static final String DEBUG_LOG = "debug_log_to_syslog";
    private static final String INFO_LOG = "info_log_to_syslog";
    private static final String WARN_LOG = "warn_log_to_syslog";
    private static final String ERROR_LOG = "error_log_to_syslog";
    private static final String FATAL_LOG = "fatal_log_to_syslog";
    private static final String EXPECTED_TRACE = "DEBUG";
    private static final String EXPECTED_DEBUG = "DEBUG";
    private static final String EXPECTED_INFO = "INFO";
    private static final String EXPECTED_WARN = "WARN";
    private static final String EXPECTED_ERROR = "ERROR";
    private static final String EXPECTED_FATAL = "EMERGENCY";

    private List<String> logs = new ArrayList<String>();
    private FileInputStream fstream;

    @Test
    public void testLoggingToSyslog() throws Exception {

        File logFile = new File(System.getProperty("java.io.tmpdir"), FILE_NAME);
        fstream = new FileInputStream(logFile);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = br.readLine()) != null) {
            logs.add(line);
        }

        containRightMessage(TRACE_LOG, EXPECTED_TRACE, "TRACE");
        containRightMessage(DEBUG_LOG, EXPECTED_DEBUG, "DEBUG");
        containRightMessage(INFO_LOG, EXPECTED_INFO, "INFO");
        containRightMessage(WARN_LOG, EXPECTED_WARN, "WARN");
        containRightMessage(ERROR_LOG, EXPECTED_ERROR, "ERROR");
        containRightMessage(FATAL_LOG, EXPECTED_FATAL, "FATAL");
        assertFalse("message on TRACE level was logged but ERROR level was set to log", containSubstringTwice(TRACE_LOG));
        assertFalse("message on DEBUG level was logged but ERROR level was set to log", containSubstringTwice(DEBUG_LOG));
        assertFalse("message on INFO level was logged but ERROR level was set to log", containSubstringTwice(INFO_LOG));
        assertFalse("message on WARN level was logged but ERROR level was set to log", containSubstringTwice(WARN_LOG));
        assertTrue("message on ERROR level wasn't log but ERROR level was set to log", containSubstringTwice(ERROR_LOG));
        assertTrue("message on FATAL level wasn't log but ERROR level was set to log", containSubstringTwice(FATAL_LOG));

    }

    @Before
    public void log() throws InterruptedException {
        LOGGER.trace(TRACE_LOG);
        LOGGER.debug(DEBUG_LOG);
        LOGGER.info(INFO_LOG);
        LOGGER.warn(WARN_LOG);
        LOGGER.error(ERROR_LOG);
        LOGGER.fatal(FATAL_LOG);
        Thread.sleep(ADJUSTED_SECOND);
    }

    @After
    public void closeStream() throws IOException {
        fstream.close();
    }

    /*
     * tests that message is logged and is logged on expected level
     */
    private void containRightMessage(String substring, String expectedLevel, String textLevel) {
        String message = "";
        boolean contain = false;
        Iterator<String> it = logs.iterator();
        while (it.hasNext()) {
            String log = (String) it.next();
            if (log.contains(substring)) {
                contain = true;
                message = log;
                break;
            }
        }
        assertTrue("message on " + textLevel + " level wasn't logged", contain);
        assertTrue("message on " + textLevel + " wasn't logged on expected level", message.contains(expectedLevel));
    }

    /*
     * It tests that log is logged twice. There are two syslog handlers and it is used to check, that both of them was logged
     * the message
     */
    private boolean containSubstringTwice(String substring) {
        Iterator<String> it = logs.iterator();
        int counter = 0;
        while (it.hasNext()) {
            String log = (String) it.next();
            if (log.contains(substring)) {
                counter++;
                if (counter == 2) {
                    return true;
                }
            }
        }
        return false;
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        //war.addPackage(SyslogHandlerTestCase.class.getPackage());
        war.addClass(TimeoutUtil.class);
        return war;
    }

    static class SyslogHandlerTestCaseSetup implements ServerSetupTask {

        private static SyslogServerIF server;
        private static File logFile;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            final int PORT = 9876;

            logFile = new File(System.getProperty("java.io.tmpdir"), FILE_NAME);

            if (logFile.exists()) {
                logFile.delete();
            }

            // start and set syslog server
            server = SyslogServer.getInstance("udp");
            server.getConfig().setPort(PORT);
            SyslogServerEventHandlerIF eventHandler = new FileSyslogServerEventHandler(logFile.getAbsolutePath(), false);
            server.getConfig().addEventHandler(eventHandler);
            SyslogServer.getThreadedInstance("udp");

            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("logger", PACKAGE);
            op.get("level").set("TRACE");
            managementClient.getControllerClient().execute(op);
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("syslog-handler", "SYSLOG");
            op.get("level").set("TRACE");
            op.get("port").set(PORT);
            managementClient.getControllerClient().execute(op);
            op = new ModelNode();
            op.get(OP).set("add-handler");
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("logger", PACKAGE);
            op.get("name").set("SYSLOG");
            managementClient.getControllerClient().execute(op);

            // second syslog handler for testing that lower messages then are
            // specified in level aren't logged
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("syslog-handler", "SYSLOG2");
            op.get("level").set("ERROR");
            op.get("port").set(PORT);
            managementClient.getControllerClient().execute(op);
            op = new ModelNode();
            op.get(OP).set("add-handler");
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("logger", PACKAGE);
            op.get("name").set("SYSLOG2");
            managementClient.getControllerClient().execute(op);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // stop syslog server
            SyslogServer.shutdown();

            // remove syslog-handler SYSLOG
            ModelNode op;
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("syslog-handler", "SYSLOG");
            managementClient.getControllerClient().execute(op);

            // remove syslog-handler SYSLOG2
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("syslog-handler", "SYSLOG2");
            managementClient.getControllerClient().execute(op);

            // remove logger
            op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("logger", PACKAGE);
            managementClient.getControllerClient().execute(op);

            // delete log file
            if (logFile.exists()) {
                logFile.delete();
            }
        }

    }
}
