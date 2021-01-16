/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.shared.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;

/**
 * Utility class for logging repetitive tasks
 * @author tmiyar
 *
 */
public class LoggingUtil {

    /**
     * Will return the full log path for the handler in the name parameter,
     * to be used when using @RunAsClient annotation
     * Depending on how you run it you might need some permissions:
     * new PropertyPermission("node0", "read"),
     * new RemotingPermission("connect"),
     * new SocketPermission(Utils.getDefaultHost(true), "accept,connect,listen,resolve"),
     * new RuntimePermission("getClassLoader")
     * @param managementClient
     * @param name of handler
     * @param handlerType i.e. periodic-rotating-file-handler
     * @return
     * @throws Exception
     */
    public static Path getLogPath(ManagementClient managementClient, String handlerType, String name) throws Exception {
        final ModelNode address = Operations.createAddress("subsystem", "logging", handlerType, name);
        final ModelNode op = Operations.createOperation("resolve-path", address);
        final ModelNode result = managementClient.getControllerClient().execute(op);
        if (!Operations.isSuccessfulOutcome(result)) {
            throw new Exception("Can't get log file");
        }
        return Paths.get(Operations.readResult(result).asString());
    }

    /**
     * Will return the full log path for the given log file relative to the the jboss.server.log.dir.
     * Meant for use by test code that runs in the server VM. Tests that use this should add
     * the following permission to allow the call to succeed in a testsuite run with the security manager enabled:
     * new PropertyPermission("jboss.server.log.dir", "read")
     *
     * @param logFile name of the log file, relative to the server log directory
     * @return the path
     */
    public static Path getInServerLogPath(String logFile) {
        return Paths.get(System.getProperty("jboss.server.log.dir")).resolve(logFile);
    }


    @SafeVarargs
    public static boolean hasLogMessage(String logFileName, String logMessage, Predicate<String>... filters) throws Exception {

        Path logPath = LoggingUtil.getInServerLogPath(logFileName);
        return isMessageInLogFile(logPath, logMessage, 0, filters);
    }

    @SafeVarargs
    public static boolean hasLogMessage(ManagementClient managementClient, String handlerName, String logMessage, Predicate<String>... filters) throws Exception {

        Path logPath = LoggingUtil.getLogPath(managementClient, "file-handler", handlerName);
        return isMessageInLogFile(logPath, logMessage, 0, filters);
    }

    @SafeVarargs
    public static boolean hasLogMessage(ManagementClient managementClient, String handlerName, String logMessage, long offset, Predicate<String>... filters) throws Exception {

        Path logPath = LoggingUtil.getLogPath(managementClient, "file-handler", handlerName);
        return isMessageInLogFile(logPath, logMessage, offset, filters);
    }

    @SafeVarargs
    private static boolean isMessageInLogFile(Path logPath, String logMessage, long offset, Predicate<String>... filters) throws Exception{
        boolean found = false;
        try (BufferedReader fileReader = Files.newBufferedReader(logPath)) {
            String line = "";
            long count = 0;
            while ((line = fileReader.readLine()) != null) {
                if (count++ < offset) {
                    continue;
                }
                if (line.contains(logMessage)) {
                    found = true;
                    for (int i = 0; found && filters != null && i < filters.length; i++) {
                        found = filters[i].test(line);
                    }
                    if (found) {
                        break;
                    }
                }
            }
        }
        return found;
    }


    /**
     * Helper method to dump the contents of a log to stdout.
     * @param logFileName the name of the log file
     */
    public static void dumpTestLog(String logFileName) throws IOException {

        Path logPath = LoggingUtil.getInServerLogPath(logFileName);
        dumpTestLog(logPath);
    }

    /**
     * Helper method to dump the contents of a log to stdout.
     * @param managementClient client to use the name of the log file used by a handler
     * @param handlerName name of the handler that writes to the file
     */
    public static void dumpTestLog(ManagementClient managementClient, String handlerName) throws Exception {

        Path logPath = LoggingUtil.getLogPath(managementClient, "file-handler", handlerName);
        dumpTestLog(logPath);
    }

    private static void dumpTestLog(Path logPath) throws IOException {
        try (BufferedReader fileReader = Files.newBufferedReader(logPath)) {
            String line = "";
            while ((line = fileReader.readLine()) != null) {
                System.out.println(line);
            }
        }

    }

    public static long countLines(Path logPath) throws Exception {
        try(Stream<String> lines = Files.lines(logPath)) {
            return lines.count();
        }
    }
}
