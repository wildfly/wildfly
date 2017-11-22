/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.appclient.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;


/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author Stuart Douglas
 */
public class AppClientWrapper implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(AppClientWrapper.class);

    private String appClientCommand = null;

    private static final String outThreadHame = "APPCLIENT-out";
    private static final String errThreadHame = "APPCLIENT-err";

    private Process appClientProcess;
    private PrintWriter writer;
    private BufferedReader outputReader;
    private BufferedReader errorReader;
    private BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();
    private Thread shutdownThread;
    private final Archive<?> archive;
    private final String clientArchiveName;
    private final String appClientArgs;
    private File archiveOnDisk;
    private final String args;

    /**
     * Creates new CLI wrapper. If the connect parameter is set to true the CLI
     * will connect to the server using <code>connect</code> command.
     *
     *
     * @param archive
     * @param clientArchiveName
     * @param args
     * @throws Exception
     */
    public AppClientWrapper(final Archive<?> archive,final String appClientArgs, final String clientArchiveName, final String args) throws Exception {
        this.archive = archive;
        this.clientArchiveName = clientArchiveName;
        this.args = args;
        this.appClientArgs = appClientArgs;
        init();
    }

    /**
     * Consumes all available output from App Client.
     *
     * @param timeout number of milliseconds to wait for each subsequent line
     * @return array of App Client output lines
     */
    public String[] readAll(final long timeout) {
        Vector<String> lines = new Vector<String>();
        String line = null;
        do {
            try {
                line = outputQueue.poll(timeout, TimeUnit.MILLISECONDS);
                if (line != null) lines.add(line);
            } catch (InterruptedException ioe) {
            }

        } while (line != null);
        return lines.toArray(new String[]{});
    }

    /**
     * Consumes all available output from CLI.
     *
     * @param timeout number of milliseconds to wait for each subsequent line
     * @return array of CLI output lines
     */
    public String readAllUnformated(long timeout) {
        String[] lines = readAll(timeout);
        StringBuilder buf = new StringBuilder();
        for (String line : lines) buf.append(line + "\n");
        return buf.toString();

    }

    /**
     * Kills the app client
     *
     * @throws Exception
     */
    public synchronized void quit() throws Exception {
        appClientProcess.destroy();
        try {
            appClientProcess.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().removeShutdownHook(shutdownThread);
        if (archiveOnDisk != null) {
            archiveOnDisk.delete();
        }
    }


    private void init() throws Exception {
        shutdownThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (appClientProcess != null) {
                    appClientProcess.destroy();
                    if (archiveOnDisk != null) {
                        archiveOnDisk.delete();
                    }
                    try {
                        appClientProcess.waitFor();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        appClientProcess = Runtime.getRuntime().exec(getAppClientCommand());
        writer = new PrintWriter(appClientProcess.getOutputStream());
        outputReader = new BufferedReader(new InputStreamReader(appClientProcess.getInputStream()));
        errorReader = new BufferedReader(new InputStreamReader(appClientProcess.getErrorStream()));

        final Thread readOutputThread = new Thread(this, outThreadHame);
        readOutputThread.start();
        final Thread readErrorThread = new Thread(this, errThreadHame);
        readErrorThread.start();

    }

    private String getAppClientCommand() throws Exception {
        if (appClientCommand != null) return appClientCommand;

        final String tempDir = System.getProperty("java.io.tmpdir");
        archiveOnDisk = new File(tempDir + File.separator + archive.getName());
        if(archiveOnDisk.exists()) {
            archiveOnDisk.delete();
        }
        final ZipExporter exporter = archive.as(ZipExporter.class);
        exporter.exportTo(archiveOnDisk);
        final String archiveArg;
        if(clientArchiveName == null) {
            archiveArg = archiveOnDisk.getAbsolutePath();
        } else {
            archiveArg = archiveOnDisk.getAbsolutePath() + "#" + clientArchiveName;
        }

        // TODO: Move to a shared testsuite lib.
        String asDist = System.getProperty("jboss.dist");
        if( asDist == null ) throw new Exception("'jboss.dist' property is not set.");
        if( ! new File(asDist).exists() ) throw new Exception("AS dir from 'jboss.dist' doesn't exist: " + asDist + " user.dir: " + System.getProperty("user.dir"));

        // TODO: Move to a shared testsuite lib.
        //String asInst = System.getProperty("jboss.inst");
        //if( asInst == null ) throw new Exception("'jboss.inst' property is not set. Perhaps this test is in a multi-node tests group but runs outside container?");
        //if( ! new File(asInst).exists() ) throw new Exception("AS dir from 'jboss.inst' doesn't exist: " + asInst + " user.dir: " + System.getProperty("user.dir"));

        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        appClientCommand = java +
                " -Djboss.modules.dir="+ asDist + "/modules" +
                " -Djline.WindowsTerminal.directConsole=false" +
                TestSuiteEnvironment.getIpv6Args() +
                "-Djboss.bind.address=" + TestSuiteEnvironment.getServerAddress() +
                " "+System.getProperty("server.jvm.args") +
                " -jar "+ asDist + "/jboss-modules.jar" +
                " -mp "+ asDist + "/modules" +
                " org.jboss.as.appclient" +
                " -Djboss.server.base.dir="+ asDist + "/appclient" +
                " -Djboss.home.dir="+ asDist +
                " " + this.appClientArgs + " " + archiveArg + " " + args;
        return appClientCommand;
    }

    /**
     *
     */
    public void run() {
        final String threadName = Thread.currentThread().getName();
        final BufferedReader reader = threadName.equals(outThreadHame) ? outputReader : errorReader;
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
            }
        }
    }

    private synchronized void outputLineReceived(String line) {
        LOGGER.trace("[" + outThreadHame + "] " + line);
        outputQueue.add(line);
    }

    private synchronized void errorLineReceived(String line) {
        LOGGER.trace("[" + outThreadHame + "] " + line);
    }

}
