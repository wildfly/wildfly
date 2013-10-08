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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.productivity.java.syslog4j.SyslogConstants.UDP;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.logging.syslogserver.BlockedSyslogServerEventHandler;
import org.jboss.as.test.integration.logging.syslogserver.UDPSyslogServerConfig;
import org.jboss.as.test.integration.logging.util.AbstractLoggingTest;
import org.jboss.as.test.integration.logging.util.LoggingServlet;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;

/**
 * A SyslogHandlerTestCase for testing that logs are logged to syslog
 * 
 * @author Ondrej Lukas
 */
@RunWith(Arquillian.class)
@ServerSetup(SyslogHandlerTestCase.SyslogHandlerTestCaseSetup.class)
@RunAsClient
public class SyslogHandlerTestCase extends AbstractLoggingTest {

    private static Logger LOGGER = Logger.getLogger(SyslogHandlerTestCase.class);

    /** prefix used for logged messages */
    private static final String LOG_PREFIX = "Syslog";
    /** Logging servlet URL with query parameter configured */
    private static final String LOGGER_QUERY = LoggingServlet.SERVLET_URL.substring(1) + "?" + LoggingServlet.PARAM_PREFIX
            + "=" + LOG_PREFIX;

    private static final PathAddress SYSLOG_PROFILE_ADDR = PathAddress.pathAddress().append(SUBSYSTEM, "logging")
            .append("logging-profile", "syslog-profile");
    private static final PathAddress SYSLOG_HANDLER_ADDR = SYSLOG_PROFILE_ADDR.append("syslog-handler", "SYSLOG");

    @ContainerResource
    private ManagementClient managementClient;

    /** Syslog server port. */
    private static final int PORT = 10514;

    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);

    /**
     * Tests that messages on all levels are logged, when level="TRACE" in syslog handler.
     */
    @Test
    public void testAllLevelLogs(@ArquillianResource URL deployementUrl) throws Exception {
        final BlockingQueue<SyslogServerEventIF> queue = BlockedSyslogServerEventHandler.getQueue();
        setSyslogAttribute("level", "TRACE");
        queue.clear();
        makeLogs(deployementUrl);
        for (Level level : LoggingServlet.LOG_LEVELS) {
            testLog(level);
        }
        final SyslogServerEventIF expectNoOtherMsg = queue.poll(3 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNull("No other message was expected in syslog.", expectNoOtherMsg);
    }

    /**
     * Tests that only messages on specific level or higher level are logged to syslog.
     */
    @Test
    public void testLogOnSpecificLevel(@ArquillianResource URL deployementUrl) throws Exception {
        final BlockingQueue<SyslogServerEventIF> queue = BlockedSyslogServerEventHandler.getQueue();
        setSyslogAttribute("level", "ERROR");
        queue.clear();
        makeLogs(deployementUrl);
        testLog(Level.ERROR);
        testLog(Level.FATAL);
        final SyslogServerEventIF expectNoOtherMsg = queue.poll(3 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNull("No other message was expected in syslog.", expectNoOtherMsg);
    }

    /**
     * Tests that there is no log if the syslog handler is disabled.
     */
    @Test
    public void testDisabledSyslog(@ArquillianResource URL deployementUrl) throws Exception {
        final BlockingQueue<SyslogServerEventIF> queue = BlockedSyslogServerEventHandler.getQueue();
        setSyslogAttribute("level", "TRACE");
        setSyslogAttribute(ENABLED, "false");
        queue.clear();
        try {
            makeLogs(deployementUrl);
            final SyslogServerEventIF expetNoMsg = queue.poll(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
            Assert.assertNull("No syslog message expected.", expetNoMsg);
        } finally {
            setSyslogAttribute(ENABLED, "true");
        }
    }

    /**
     * Tests if the next message in the syslog is the expected one with the given log-level.
     * 
     * @param expectedLevel
     * @throws Exception
     */
    private void testLog(final Logger.Level expectedLevel) throws Exception {
        SyslogServerEventIF log = BlockedSyslogServerEventHandler.getQueue().poll(15 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        assertNotNull(log);
        assertEquals("Message with unexpected Syslog event level received.", getSyslogLevel(expectedLevel), log.getLevel());
        final String expectedMsg = MessageFormat.format(LoggingServlet.MSG_TEMPLATE, LoggingServlet.getPrefix(LOG_PREFIX),
                LoggingServlet.getLevelStr(expectedLevel));
        String msg = log.getMessage();
        assertEquals("Message with unexpected Syslog event text received.", expectedMsg, msg);
    }

    /**
     * Convert JBoss Logger.Level to Syslog log level.
     * 
     * @param jbossLogLevel
     * @return
     */
    private int getSyslogLevel(Level jbossLogLevel) {
        final int result;
        switch (jbossLogLevel) {
            case TRACE:
            case DEBUG:
                result = SyslogConstants.LEVEL_DEBUG;
                break;
            case INFO:
                result = SyslogConstants.LEVEL_INFO;
                break;
            case WARN:
                result = SyslogConstants.LEVEL_WARN;
                break;
            case ERROR:
                result = SyslogConstants.LEVEL_ERROR;
                break;
            case FATAL:
                result = SyslogConstants.LEVEL_EMERGENCY;
                break;
            default:
                // unexpected
                result = SyslogConstants.LEVEL_CRITICAL;
                break;
        }
        return result;
    }

    /**
     * Sets a single attribute of the syslog handler in AS configuration.
     * 
     * @param attribute
     * @param level
     * @throws Exception
     */
    private void setSyslogAttribute(String attribute, String level) throws Exception {
        final ModelNode op = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, SYSLOG_HANDLER_ADDR);
        op.get(NAME).set(attribute);
        op.get(VALUE).set(level);
        Utils.applyUpdate(op, managementClient.getControllerClient());
    }

    /**
     * Get request for {@link LoggingServlet}, which creates log entries.
     * 
     * @param deployementUrl
     * @throws MalformedURLException
     * @throws IOException
     */
    private void makeLogs(final URL deployementUrl) throws MalformedURLException, IOException {
        URL url = new URL(deployementUrl, LOGGER_QUERY);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        int statusCode = http.getResponseCode();
        assertTrue("Invalid response statusCode: " + statusCode, statusCode == HttpServletResponse.SC_OK);
    }

    @Deployment
    public static WebArchive deployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war");
        war.addClasses(LoggingServlet.class);
        war.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                StringBuffer dependencies = new StringBuffer();
                builder.addManifestHeader("Dependencies", dependencies.toString());
                builder.addManifestHeader("Logging-Profile", "syslog-profile");
                return builder.openStream();
            }
        });
        return war;
    }

    static class SyslogHandlerTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            LOGGER.info("starting syslog server on port " + PORT);

            // clear created server instances (TCP/UDP)
            SyslogServer.shutdown();
            // create a new UDP instance
            final String host = Utils.getHost(managementClient);
            final UDPSyslogServerConfig config = new UDPSyslogServerConfig();
            config.setPort(PORT);
            config.setHost(host);
            config.setUseStructuredData(true);
            config.addEventHandler(new BlockedSyslogServerEventHandler());
            SyslogServer.createInstance(UDP, config);
            // start syslog server
            SyslogServer.getThreadedInstance(SyslogConstants.UDP);

            final ModelNode compositeOp = new ModelNode();
            compositeOp.get(OP).set(COMPOSITE);
            compositeOp.get(OP_ADDR).setEmptyList();
            final ModelNode steps = compositeOp.get(STEPS);

            // create syslog-profile
            steps.add(Util.createAddOperation(SYSLOG_PROFILE_ADDR));

            ModelNode op = Util.createAddOperation(SYSLOG_HANDLER_ADDR);
            op.get("level").set("TRACE");
            op.get("port").set(PORT);
            op.get("server-address").set(host);
            op.get("enabled").set("true");
            steps.add(op);

            op = Util.createAddOperation(SYSLOG_PROFILE_ADDR.append("root-logger", "ROOT"));
            op.get("level").set("TRACE");
            op.get("handlers").add("SYSLOG");
            steps.add(op);

            Utils.applyUpdate(compositeOp, managementClient.getControllerClient());

            LOGGER.info("syslog server setup complete");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // stop syslog server
            LOGGER.info("stopping syslog server");
            SyslogServer.shutdown();
            LOGGER.info("syslog server stopped");

            // remove syslog-profile
            final ModelNode op = Util.createRemoveOperation(SYSLOG_PROFILE_ADDR);
            op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            Utils.applyUpdate(op, managementClient.getControllerClient());
            LOGGER.info("syslog server logging profile removed");
        }
    }
}
