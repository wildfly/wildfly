/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.process;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.net.ServerSocketFactory;

import org.jboss.as.protocol.old.ProtocolServer;
import org.jboss.logging.MDC;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.threads.JBossThreadFactory;

/**
 * The main entry point for the process controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Main {

    private Main() {
    }

    public static final String HOST_CONTROLLER_PROCESS_NAME = "Host Controller";
    public static final String HOST_CONTROLLER_MODULE = "org.jboss.as.host-controller";

    public static void main(String[] args) throws IOException {

        start(args);
    }

    public static ProcessController start(String[] args) throws IOException {
        MDC.put("process", "process controller");

        String javaHome = System.getProperty("java.home", ".");
        String jvmName = javaHome + "/bin/java";
        String jbossHome = System.getProperty("jboss.home.dir", ".");
        String modulePath = System.getProperty("jboss.module.path", "modules");
        String bootJar = "jboss-modules.jar";
        String logModule = "org.jboss.logmanager";
        String jaxpModule = "javax.xml.jaxp-provider";
        String bootModule = HOST_CONTROLLER_MODULE;
        String bindAddress = "127.0.0.1";
        int bindPort = 0;

        final List<String> javaOptions = new ArrayList<String>();
        final List<String> smOptions = new ArrayList<String>();

        // logmodule is the same as mine or defaulted
        // target module is always SM
        // -mp is my module path
        // -jar is jboss-modules.jar in jboss-home
        // log config should be fixed loc

        OUT: for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-jvm".equals(arg)) {
                jvmName = args[++i];
            } else if ("-jboss-home".equals(arg)) {
                jbossHome = args[++i];
            } else if ("-mp".equals(arg)) {
                modulePath = args[++i];
            } else if ("-jar".equals(arg)) {
                bootJar = args[++i];
            } else if ("-logmodule".equals(arg)) {
                logModule = args[++i];
            } else if ("-jaxpmodule".equals(arg)) {
                jaxpModule = args[++i];
            } else if ("-bind-addr".equals(arg)) {
                bindAddress = args[++i];
            } else if ("-bind-port".equals(arg)) {
                bindPort = Integer.parseInt(args[++i]);
            } else if ("--".equals(arg)) {
                for (i++; i < args.length; i++) {
                    arg = args[i];
                    if ("--".equals(arg)) {
                        for (i++; i < args.length; i++) {
                            arg = args[i];
                            smOptions.add(arg);
                        }
                        break OUT;
                    } else {
                        javaOptions.add(arg);
                    }
                }
                break OUT;
            } else {
                throw new IllegalArgumentException("Bad option: " + arg);
            }
        }

        Handler consoleHandler = null;

        final Logger rootLogger = Logger.getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                if (consoleHandler != null) {
                    // duplicate handlers
                    rootLogger.removeHandler(handler);
                } else {
                    consoleHandler = handler;
                    ((ConsoleHandler)consoleHandler).setWriter(new SynchronizedWriter(System.out));
                }
            }
        }

        final ProtocolServer.Configuration configuration = new ProtocolServer.Configuration();
        if (bindAddress != null) {
            configuration.setBindAddress(new InetSocketAddress(bindAddress, bindPort));
        } else {
            configuration.setBindAddress(new InetSocketAddress(bindPort));
        }
        // todo better config
        configuration.setBindAddress(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        configuration.setSocketFactory(ServerSocketFactory.getDefault());
        final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("ProcessController-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
        configuration.setThreadFactory(threadFactory);
        configuration.setReadExecutor(Executors.newCachedThreadPool(threadFactory));

        final ProcessController processController = new ProcessController(configuration, System.out, System.err);
        final InetSocketAddress boundAddress = processController.getServer().getBoundAddress();

        final List<String> initialCommand = new ArrayList<String>();
        initialCommand.add(jvmName);
        initialCommand.add("-D" + "jboss.home.dir=" + jbossHome);

        initialCommand.addAll(javaOptions);
        initialCommand.add("-jar");
        initialCommand.add(bootJar);
        initialCommand.add("-mp");
        initialCommand.add(modulePath);
        initialCommand.add("-logmodule");
        initialCommand.add(logModule);
        initialCommand.add("-jaxpmodule");
        initialCommand.add(jaxpModule);
        initialCommand.add(bootModule);
        initialCommand.add(CommandLineConstants.INTERPROCESS_PC_ADDRESS);
        initialCommand.add(boundAddress.getHostName());
        initialCommand.add(CommandLineConstants.INTERPROCESS_PC_PORT);
        initialCommand.add(Integer.toString(boundAddress.getPort()));
        initialCommand.addAll(smOptions);

        processController.addProcess(HOST_CONTROLLER_PROCESS_NAME, initialCommand, Collections.<String, String>emptyMap(), jbossHome, true);
        processController.startProcess(HOST_CONTROLLER_PROCESS_NAME);

        final Thread shutdownThread = new Thread(new Runnable() {
            public void run() {
                processController.shutdown();
            }
        }, "Shutdown thread");
        shutdownThread.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        return processController;
    }
}
