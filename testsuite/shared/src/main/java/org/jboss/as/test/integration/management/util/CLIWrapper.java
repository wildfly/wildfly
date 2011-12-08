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
package org.jboss.as.test.integration.management.util;

import static org.jboss.as.arquillian.container.Authentication.PASSWORD;
import static org.jboss.as.arquillian.container.Authentication.USERNAME;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class CLIWrapper implements Runnable {

    private static String cliCommand = null;

    private static final String outThreadHame = "CLI-out";
    private static final String errThreadHame = "CLI-err";

    private Process cliProcess;
    private PrintWriter writer;
    private BufferedReader outputReader;
    private BufferedReader errorReader;
    private BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();

    /**
     * Creates new CLI wrapper.
     * @throws Exception
     */
    public CLIWrapper() throws Exception {
        this(false);
    }

    /**
     * Creates new CLI wrapper. If the connect parameter is set to true the CLI
     * will connect to the server using <code>connect</code> command.
     * @param connect indicates if the CLI should connect to server automatically.
     * @throws Exception
     */
    public CLIWrapper(boolean connect) throws Exception {
        init();
        if (!connect) return;

        //connect

        // wait for cli welcome message
        String line = readLine(10000);
        
        while(! line.contains("You are disconnected")) {
            line = readLine(10000);
        }
        
        sendLine("connect", false);
        line = readLine(5000);

        assertTrue("Check we are disconnected:" + line, line.indexOf("disconnected") >= 0);
        sendLine("version", false);
        line = readLine(5000);
        assertTrue("Connect failed:" + line, line.indexOf("[standalone@") >= 0);

    }

    /**
     * Sends command line to CLI.
     * @param line specifies the command line.
     * @param waitForEcho if set to true reads the echo response form the CLI.
     * @throws Exception
     */
    public void sendLine(String line, boolean waitForEcho) throws Exception {
        System.out.println("[CLI-inp] " + line);
        writer.println(line);
        writer.flush();

        if (! waitForEcho) return;

        boolean found = false;
        StringBuilder lines = new StringBuilder();
        while (! found) {
            String eLine = readLine(5000);
            if (eLine == null) throw new Exception("CLI command failed. Sent:" + line + ", received:" + lines.toString());
            lines.append(eLine);
            lines.append(System.getProperty("line.separator"));
            if (eLine.indexOf(line) >= 0) found = true;
        }
    }

    /**
     * Sends command line to CLI.
     * @param line specifies the command line.
     * @throws Exception
     */
    public void sendLine(String line) throws Exception {
        sendLine(line, true);
    }


    /**
     * Non blocking read from CLI output.
     * @return next line from CLI output or null if the output is empty
     */
    public String readLine() {
        return outputQueue.poll();
    }

    /**
     * Blocking read from CLI output.
     * @param timeout number of milliseconds to wait for line
     * @return next line from CLI output
     * @throws Exception is thrown if there is no output available and timeout expired
     */
    public String readLine(long timeout) throws Exception {
        String line = null;
        try {
            line = outputQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ioe) {}
        if (line == null) throw new Exception("CLI read timeout.");
        return line;
    }

    /**
     * Consumes all available output from CLI.
     * @param timeout number of milliseconds to wait for first line
     * @param lineTimeout number of milliseconds to wait for each subsequent line
     * @return array of CLI output lines
     */
    public String[] readAll(long timeout, long lineTimeout) {
        Vector<String> lines = new Vector<String>();
        try {
            String line = outputQueue.poll(timeout, TimeUnit.MILLISECONDS);
            while (line != null) {
                lines.add(line);
                line = outputQueue.poll(lineTimeout, TimeUnit.MILLISECONDS);                
            }
        } catch (InterruptedException ioe) {}

        return lines.toArray(new String[]{});
    }

    /**
     * Consumes all available output from CLI.
     * @param timeout number of milliseconds to wait for first line
     * @param lineTimeout number of milliseconds to wait for each subsequent line
     * @return array of CLI output lines
     */
    public String readAllUnformated(long timeout, long lineTimeout) {
        String[] lines = readAll(timeout, lineTimeout);
        StringBuilder buf = new StringBuilder();
        for(String line : lines) buf.append(line + " ");
        return buf.toString();

    }

    /**
     * Consumes all available output from CLI and converts the output to ModelNode operation format     
     * @param timeout number of milliseconds to wait for first line
     * @param lineTimeout number of milliseconds to wait for each subsequent line
     * @return array of CLI output lines
     */    
    public CLIOpResult readAllAsOpResult(long timeout, long lineTimeout) throws Exception {
        String output = readAllUnformated(timeout, lineTimeout);
        StreamTokenizer st = new StreamTokenizer(new StringReader(output));
        st.resetSyntax();
        st.whitespaceChars(' ', ' ');
        st.wordChars('#', '+');
        st.wordChars('-', 'Z');
        st.wordChars('a', 'z');
        st.quoteChar('"');

        int token = st.nextToken();
        assertTrue ("{ expected.", token == '{');
        Map compound = parseCompound(st);
        CLIOpResult res = new CLIOpResult();
        res.setIsOutcomeSuccess("success".equals(compound.get("outcome")));
        res.setResult(compound.get("result"));
        return res;
    }

    private Map<String, Object> parseCompound(StreamTokenizer st) throws IOException, ParseException {
        Map<String, Object> map = new HashMap<String, Object>();

        int token = st.nextToken();
        while (token != '}') {
            String key = st.sval;
            st.nextToken();
            if (! "=>".equals(st.sval)) throw new ParseException("=> expected, got:" + st.sval, st.lineno());
            token = st.nextToken();
            if (token == '{') {
                // compound attribute
                map.put(key, parseCompound(st));
            } else if (token == '[') {
                // list attribure
                map.put(key, parseList(st));
            } else {
                // primitive attribute
                map.put(key, st.sval);
            }
            token = st.nextToken();
            if (token == ',') token = st.nextToken();
        }
        return map;
    }

    private List parseList(StreamTokenizer st) throws IOException, ParseException {
        List list = new LinkedList();

        int token = st.nextToken();
        while (token != ']') {
            if (token == '{') {
                // compound attribute
                list.add(parseCompound(st));
            } else if (token == '[') {
                // list attribure
                list.add(parseList(st));
            } else {
                // primitive attribute
                list.add(st.sval);
            }
            token = st.nextToken();
            if (token == ',') token = st.nextToken();
        }
        return list;
    }

    /**
     * Discards all CLI output.
     */
    public void flush() {
        outputQueue.clear();
    }

    /**
     * Sends quit command to CLI.
     * @throws Exception
     */
    public synchronized void quit() throws Exception {
        sendLine("quit", false);
        while ((outputReader != null) || (errorReader != null))
            try {
                wait();
            } catch (InterruptedException ie) {

            }

    }


    private void init() throws Exception {
        System.out.println("CLI command:" + getCliCommand());

        cliProcess = Runtime.getRuntime().exec(getCliCommand());
        writer = new PrintWriter(cliProcess.getOutputStream());
        outputReader = new BufferedReader(new InputStreamReader(cliProcess.getInputStream()));
        errorReader = new BufferedReader(new InputStreamReader(cliProcess.getErrorStream()));

        Thread readOutputThread = new Thread(this, outThreadHame);
        readOutputThread.start();
        Thread readErrorThread = new Thread(this, errThreadHame);
        readErrorThread.start();

    }

    private static String getCliCommand() throws Exception {
        if (cliCommand != null) return cliCommand;

        String asDist = System.getProperty("jboss.dist");
        String asInst = System.getProperty("jboss.inst");

        String javaExec = System.getProperty("java.home") + File.separatorChar + "bin" + File.separatorChar + "java";
        if (javaExec.contains(" ")) {
            javaExec = "\"" + javaExec + "\"";
        }

        cliCommand = javaExec + " -Djboss.home.dir=" + asInst +
            " -Djboss.modules.dir=" + asDist + "/modules" +
            " -Djline.WindowsTerminal.directConsole=false" +
            " -jar " + asDist + "/jboss-modules.jar" +
            " -mp "  + asDist + "/modules" +
            " -logmodule org.jboss.logmanager org.jboss.as.cli" +
            " --user=" + USERNAME +
            " --password=" + PASSWORD;
        return cliCommand;
    }

    /**
     *
     */
    public void run() {
        String threadName = Thread.currentThread().getName();
        BufferedReader reader = threadName.equals(outThreadHame) ? outputReader : errorReader;
        try {
            String line = reader.readLine();
            while (line != null) {
                if (threadName.equals(outThreadHame))
                    outputLineReceived(line);
                else
                    errorLineReceived(line);
                line = reader.readLine();
            }
        } catch (Exception e) {
        } finally {
            synchronized (this) {
                if (threadName.equals(outThreadHame))
                    outputReader = null;
                else
                    errorReader = null;
                notifyAll();
            }
        }
    }

    private synchronized void outputLineReceived(String line) {
        System.out.println("[" + outThreadHame + "] " + line);
        outputQueue.add(line);
        notifyAll();
    }

    private synchronized void errorLineReceived(String line) {
        System.out.println("[" + outThreadHame + "] " + line);
        notifyAll();
    }

}
