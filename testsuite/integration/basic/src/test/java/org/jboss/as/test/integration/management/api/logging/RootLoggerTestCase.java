/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.api.logging;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RootLoggerTestCase extends AbstractMgmtTestBase {

    @ArquillianResource
    URL url;

    @Deployment
    public static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "LoggingServlet.war");
        war.addClass(LoggingServlet.class);
        return war;
    }

    @Before
    public void before() throws IOException {
        initModelControllerClient(url.getHost(), MGMT_PORT);
    }

    @AfterClass
    public static void after() throws IOException {
        closeModelControllerClient();
    }

    @Test
    public void testChangeRootLogLevel() throws Exception {

        // add new file loger so we can track logged messages
        File logFile = new File(tempDir, "test-fh.log");
        if (logFile.exists()) assertTrue(logFile.delete());
        addFileHandler("test-logger", "TRACE", logFile, true);

        Level[] levels = new Logger.Level[] {Level.ERROR, Level.WARN,
            Level.INFO, Level.DEBUG, Level.TRACE};
        Map<Level, Integer> levelOrd = new HashMap<Level,Integer>();
        levelOrd.put(Level.FATAL, 0);
        levelOrd.put(Level.ERROR, 1);
        levelOrd.put(Level.WARN, 2);
        levelOrd.put(Level.INFO, 3);
        levelOrd.put(Level.DEBUG, 4);
        levelOrd.put(Level.TRACE, 5);

        // log messages on all levels with different root logger level settings
        for(Level level : levels) {
            // change root log level
            ModelNode op = createOpNode("subsystem=logging/root-logger=ROOT", "change-root-log-level");
            op.get("level").set(level.name());
            ModelNode ret = executeOperation(op);

            // log a message
            String response = HttpRequest.get(url.toString() + "/LoggingServlet?msg=RootLoggerTestCaseTST%20" +
                    level.name(), 10, TimeUnit.SECONDS);
            assertTrue(response.contains("RootLoggerTestCaseTST"));
        }

        // stop logger
        removeFileHandler("test-logger", true);

        // go through loggeded messages - test that with each root logger level settings
        // message with equal priority and also messags with all higher
        // priorities were logged

        boolean[][] logFound = new boolean[levelOrd.size()][levelOrd.size()];

        List<String> logLines = FileUtils.readLines(logFile);
        for(String line : logLines) {
            if (! line.contains("RootLoggerTestCaseTST")) continue; // not our log
            String[] lWords = line.split(" +");
            try {
                Level lineLogLevel = Level.valueOf(lWords[1]);
                Level rootLogLevel = Level.valueOf(lWords[5]);
                int ll = levelOrd.get(lineLogLevel);
                int rl = levelOrd.get(rootLogLevel);
                assertTrue(ll <= rl);
                logFound[ll][rl] = true;
            } catch (Exception e) {
                throw new Exception("Unexpected log:" + line);
            }
        }
        for(Level level : levels) {
            int rl = levelOrd.get(level);
            for(int ll = 0; ll <= rl; ll++) assertTrue(logFound[ll][rl]);
        }

    }

    @Test
    @Ignore("AS7-2385")
    public void testSetRootLogger() throws Exception {

        // add new file loger so we can test root logger change
        File logFile = new File(tempDir, "test-fh.log");
        if (logFile.exists()) assertTrue(logFile.delete());
        addFileHandler("test-logger", "TRACE", logFile, false);

        // read root logger
        ModelNode op = createOpNode("subsystem=logging", "read-attribute");
        op.get("name").set("root-logger");
        ModelNode rootLogger = executeOperation(op);
        List<String> handlers = ModelUtil.modelNodeAsStingList(rootLogger.get("handlers"));

        // set new root logger
        op = createOpNode("subsystem=logging", "set-root-logger");
        op.get("level").set(rootLogger.get("level"));
        for(String handler : handlers) op.get("handlers").add(handler);
        op.get("handlers").add("test-logger");
        executeOperation(op);

        // force server to issue a log message
        String response = HttpRequest.get(url.toString() + "/LoggingServlet", 10, TimeUnit.SECONDS);
        assertTrue(response.contains("Logging servlet."));

        // revert root logger
        op = createOpNode("subsystem=logging", "set-root-logger");
        op.get("level").set(rootLogger.get("level"));
        op.get("handlers").set(rootLogger.get("handlers"));
        executeOperation(op);

        // remove file handler
        removeFileHandler("test-logger", false);

        // check that root logger were changed - file logger was registered
        String log = FileUtils.readFileToString(logFile);
        assertTrue(log.contains("Logging servlet."));

        // remove log file
        assertTrue(logFile.delete());
    }

    private void addFileHandler(String name, String level, File file, boolean assign) throws Exception {

        // add file handler
        ModelNode op = createOpNode("subsystem=logging/file-handler=" + name, "add");
        op.get("name").set(name);
        op.get("level").set(level);
        op.get("file").get("path").set(file.getAbsolutePath());
        executeOperation(op);

        if (!assign) return;

        // register it with root logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-assign-handler");
        op.get("name").set(name);
        executeOperation(op);
    }

    private void removeFileHandler(String name, boolean unassign) throws Exception {

        if (unassign) {
            // deregister handler from logger
            ModelNode op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-unassign-handler");
            op.get("name").set(name);
            executeOperation(op);
        }

        // remove handler
        ModelNode op = createOpNode("subsystem=logging/file-handler=" + name, "remove");
        executeOperation(op);
    }

}
