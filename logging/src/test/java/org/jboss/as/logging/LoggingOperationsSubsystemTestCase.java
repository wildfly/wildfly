/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LoggingOperationsSubsystemTestCase extends AbstractSubsystemTest {

    private static File logDir;

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    private static final Logger log = Logger.getLogger(LoggingOperationsSubsystemTestCase.class.getName());

    public LoggingOperationsSubsystemTestCase() {
        super(LoggingExtension.SUBSYSTEM_NAME, new LoggingExtension());
    }

    @BeforeClass
    public static void setupLoggingDir() {
        logDir = new File("target/logs");
        logDir.mkdirs();
        for (String name : logDir.list()) {
            new File(logDir, name).delete();
        }
        System.setProperty("jboss.server.log.dir", logDir.getAbsolutePath());
    }

    @Test
    public void testChangeRootLogLevel() throws Exception {

        KernelServices kernelServices = installInController(readResource("/operations.xml"));

        // add new file loger so we can track logged messages
        File logFile = new File(logDir, "test-fh.log");
        if (logFile.exists()) assertTrue(logFile.delete());
        addFileHandler(kernelServices, "test-logger", "TRACE", logFile, true);

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
            ModelNode ret = kernelServices.executeOperation(op);

            // log a message
            for (Logger.Level lvl : Logger.Level.values()) {
                log.log(lvl, "RootLoggerTestCaseTST " + level);
            }
        }

        // stop logger
        removeFileHandler(kernelServices, "test-logger", true);

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

        KernelServices kernelServices = installInController(readResource("/operations.xml"));

        // add new file loger so we can test root logger change
        File logFile = new File(logDir, "test-fh.log");
        if (logFile.exists()) assertTrue(logFile.delete());
        addFileHandler(kernelServices, "test-logger", "TRACE", logFile, false);

        // read root logger
        ModelNode op = createOpNode("subsystem=logging", "read-attribute");
        op.get("name").set("root-logger");
        ModelNode rootLogger = kernelServices.executeOperation(op);
        List<String> handlers = modelNodeAsStringList(rootLogger.get("handlers"));

        // set new root logger
        op = createOpNode("subsystem=logging", "set-root-logger");
        op.get("level").set(rootLogger.get("level"));
        for(String handler : handlers) op.get("handlers").add(handler);
        op.get("handlers").add("test-logger");
        kernelServices.executeOperation(op);

        // log a message
        for (Logger.Level lvl : Logger.Level.values())
            log.log(lvl, "Test123");


        // revert root logger
        op = createOpNode("subsystem=logging", "set-root-logger");
        op.get("level").set(rootLogger.get("level"));
        op.get("handlers").set(rootLogger.get("handlers"));
        kernelServices.executeOperation(op);

        // remove file handler
        removeFileHandler(kernelServices, "test-logger", false);

        // check that root logger were changed - file logger was registered
        String log = FileUtils.readFileToString(logFile);
        assertTrue(log.contains("Test123."));

        // remove log file
        assertTrue(logFile.delete());
    }

    @Test
    public void testAddRemoveFileHandler() throws Exception {
        KernelServices kernelServices = installInController(readResource("/operations.xml"));

        File logFile = new File(logDir, "test-fh.log");
        if (logFile.exists()) assertTrue(logFile.delete());

        // add file handler
        ModelNode op = createOpNode("subsystem=logging/file-handler=test-fh", "add");
        op.get("name").set("test-fh");
        op.get("level").set("INFO");
        op.get("file").get("path").set(logFile.getAbsolutePath());
        kernelServices.executeOperation(op);

        // register it with root logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-assign-handler");
        op.get("name").set("test-fh");
        kernelServices.executeOperation(op);

        // check it is listed in root-logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "read-attribute");
        op.get("name").set("handlers");
        ModelNode handlers = kernelServices.executeOperation(op).require(RESULT);
        List<String> loggers = modelNodeAsStringList(handlers);
        assertTrue(loggers.contains("test-fh"));

        for (Logger.Level level : Logger.Level.values()) {
            log.log(level, "Test123");
        }

        // deregister handler from logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-unassign-handler");
        op.get("name").set("test-fh");
        kernelServices.executeOperation(op);

        // check it is not listed in root-logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "read-attribute");
        op.get("name").set("handlers");
        handlers = kernelServices.executeOperation(op);
        loggers = modelNodeAsStringList(handlers);
        assertFalse(loggers.contains("test-fh"));

        // remove handler
        op = createOpNode("subsystem=logging/file-handler=test-fh", "remove");
        kernelServices.executeOperation(op);

        // check generated log file
        assertTrue(FileUtils.readFileToString(logFile).contains("Test123"));

        // verify that the logger is stopped, no more logs are comming to the file
        long checksum = FileUtils.checksumCRC32(logFile);
        for (Logger.Level level : Logger.Level.values()) {
            log.log(level, "Test123");
        }
        assertEquals(checksum, FileUtils.checksumCRC32(logFile));

        // remove log file
        assertTrue(logFile.delete());
    }



    private void addFileHandler(KernelServices kernelServices, String name, String level, File file, boolean assign) throws Exception {

        // add file handler
        ModelNode op = createOpNode("subsystem=logging/file-handler=" + name, "add");
        op.get("name").set(name);
        op.get("level").set(level);
        op.get("file").get("path").set(file.getAbsolutePath());
        kernelServices.executeOperation(op);

        if (!assign) return;

        // register it with root logger
        op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-assign-handler");
        op.get("name").set(name);
        kernelServices.executeOperation(op);
    }

    private void removeFileHandler(KernelServices kernelServices, String name, boolean unassign) throws Exception {

        if (unassign) {
            // deregister handler from logger
            ModelNode op = createOpNode("subsystem=logging/root-logger=ROOT", "root-logger-unassign-handler");
            op.get("name").set(name);
            kernelServices.executeOperation(op);
        }

        // remove handler
        ModelNode op = createOpNode("subsystem=logging/file-handler=" + name, "remove");
        kernelServices.executeOperation(op);
    }

    public static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String [] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private static List<String> modelNodeAsStringList(ModelNode node) {
        List<String> ret = new LinkedList<String>();
        for (ModelNode n : node.asList()) ret.add(n.asString());
        return ret;
    }

}
