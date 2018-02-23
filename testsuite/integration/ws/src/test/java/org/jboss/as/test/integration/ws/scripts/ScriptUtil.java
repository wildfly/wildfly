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
package org.jboss.as.test.integration.ws.scripts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.jboss.logging.Logger;
import org.junit.Before;


/**
 */
public class ScriptUtil {
    protected static Logger log = Logger.getLogger(ScriptUtil.class.getName());
    private static final String SYSPROP_TEST_RESOURCES_DIRECTORY = "test.resources.directory";
    private static final String testResourcesDir = System.getProperty(SYSPROP_TEST_RESOURCES_DIRECTORY);

    public static final String PS = System.getProperty("path.separator"); // ':' on unix, ';' on windows
    public static final String EXT = ":".equals( PS ) ? ".sh" : ".bat";

    public static final String MAVEN_REPO_LOCAL = System.getProperty("maven.repo.local");

    public String ENDPOINT_CLASS;

    public String JBOSS_HOME;
    public String TEST_DIR;
    public String SECURITY_POLICY;
    public String WSPROVIDE_INPUT_DIR;
    public String ABSOUTPUT;

    @Before
    public void setup() throws Exception {
        JBOSS_HOME = System.getProperty("jboss.dist");
        ENDPOINT_CLASS = "org.jboss.as.testsuite.integration.scripts.test.tools.Echo1Impl";
        TEST_DIR = Paths.get(testResourcesDir).toFile().getAbsolutePath();
        WSPROVIDE_INPUT_DIR = Paths.get(TEST_DIR, "ws", "scripts").toFile().getAbsolutePath();
        SECURITY_POLICY = Paths.get(WSPROVIDE_INPUT_DIR, "Echo1-security.policy").toFile().getAbsolutePath();
        ABSOUTPUT = TEST_DIR;
    }

    /**
     * Execute <b>command</b> in separate process, copy process input to <b>os</b>. If process will fail, display custom <b>message</b> in assertion.
     * @param command command to execute
     * @param os output stream to copy process input to. If null, <b>System.out</b> will be used
     * @param message message to display if assertion fails
     * @param env environment
     * @throws IOException if I/O error occurs
     */
    public static String executeCommand(String command, OutputStream os,
                                        String message, Map<String, String> env) throws IOException, InterruptedException {
        if (command == null)
            throw new NullPointerException( "Command cannot be null" );

        log.info("Executing command: " + command);

        StringTokenizer st = new StringTokenizer(command, " \t\r");
        List<String> tokenizedCommand = new LinkedList<String>();
        while (st.hasMoreTokens()) {
            // PRECONDITION: command doesn't contain whitespaces in the paths
            tokenizedCommand.add(st.nextToken());
        }

        String result = "";
        try {
            result = executeCommand(tokenizedCommand, os, message, env);
        }
        catch (IOException e) {
            log.warn("Make sure there are no whitespaces in command paths", e);
            throw e;
        }
        return result;
    }

    /**
     * Execute <b>command</b> in separate process, copy process input to <b>os</b>. If process will fail, display custom <b>message</b> in assertion.
     * @param command command to execute
     * @param os output stream to copy process input to. If null, <b>System.out</b> will be used
     * @param message message to display if assertion fails
     * @param env environment
     * @throws IOException if I/O error occurs
     */
    private static String executeCommand(List<String> command, OutputStream os,
                                         String message, Map<String, String> env) throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File ("/store"));
        if (env != null) {
            for (String variable : env.keySet()) {
                pb.environment().put(variable, env.get(variable));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mydebug());
        String result = "";
        Process process = pb.start();
        try {
            int errCode = process.waitFor();
            sb.append("errCode: " + errCode + " \n");
            result = output(process.getInputStream());
            sb.append("inputStream: ");
            sb.append(result);
            sb.append("\n");
            result = output(process.getErrorStream());
            sb.append("errorStream: ");
            sb.append(result);
            sb.append("\n");
        } catch (InterruptedException ie) {
            ie.printStackTrace(System.err);
            throw ie;
        } finally {
            process.destroy();
        }
        return sb.toString();
    }

    private static String mydebug() throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ls");  ///repository
        pb.directory(new File ("/store"));
        StringBuilder sb = new StringBuilder();
        sb.append("\nCMD: list dir: ");
        String result = "";
        Process process = pb.start();
        try {
            int errCode = process.waitFor();
            sb.append("errCode: " + errCode + " \n");
            result = output(process.getInputStream());
            sb.append("inputStream: ");
            sb.append(result);
            sb.append("\n");
            result = output(process.getErrorStream());
            sb.append("errorStream: ");
            sb.append(result);
            sb.append("\n\n");
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw e;
        } catch (InterruptedException ie) {
            ie.printStackTrace(System.err);
            throw ie;
        } finally {
            process.destroy();
        }
        return sb.toString();
    }

    private static String output(InputStream inputStream) throws IOException {

        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + System.getProperty("line.separator"));
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    /** Try to discover the File for the test resource */
    public static Path getResourceFile(Path resource) {

        if (resource.toFile().exists()) {
            return resource;
        }

        Path path = Paths.get(testResourcesDir, resource.toString());
        if (path.toFile().exists()) {
            return path;
        }

        String notSet = (testResourcesDir == null ? " System property '"
            + SYSPROP_TEST_RESOURCES_DIRECTORY + "' not set." : "");
        throw new IllegalArgumentException("Cannot obtain '" + testResourcesDir
            + "/" + resource + "'." + notSet);
    }
}