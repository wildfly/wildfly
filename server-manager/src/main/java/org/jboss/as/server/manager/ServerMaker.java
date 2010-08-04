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

package org.jboss.as.server.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.model.JvmElement;
import org.jboss.as.model.PropertiesElement;
import org.jboss.as.model.Standalone;
import org.jboss.as.process.ProcessManagerSlave;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServerMaker {
    
    private final ProcessManagerSlave processManagerSlave;
    private final MessageHandler messageHandler;
    private final ServerManagerEnvironment environment;
    
    public ServerMaker(ServerManagerEnvironment environment, 
            ProcessManagerSlave processManagerSlave, 
            MessageHandler messageHandler) {
        
        if (environment == null) {
            throw new IllegalArgumentException("environment is null");
        }
        this.environment = environment;
        
        if (processManagerSlave == null) {
            throw new IllegalArgumentException("processManagerSlave is null");
        }
        this.processManagerSlave = processManagerSlave;
        
        if (messageHandler == null) {
            throw new IllegalArgumentException("messageHandler is null");
        }
        this.messageHandler = messageHandler;
    }
    
    public Server makeServer(Standalone serverConfig) throws IOException {
//        final List<String> args = new ArrayList<String>();
//        if (false) {
//            // Example: run at high priority on *NIX
//            args.add("/usr/bin/nice");
//            args.add("-n");
//            args.add("-10");
//        }
//        if (false) {
//            // Example: run only on processors 1-4 on Linux
//            args.add("/usr/bin/taskset");
//            args.add("0x0000000F");
//        }
//        args.add("/home/david/local/jdk/home/bin/java");
//        args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
//        args.add("-jar");
//        args.add("jboss-modules.jar");
//        args.add("-mp");
//        args.add("modules");
//        args.add("org.jboss.as:server");
//        ProcessBuilder builder = new ProcessBuilder(args);
//        builder.redirectErrorStream(false);
//        final Process process = builder.start();
//
//        // Read errors from here, pass to logger
//        final InputStream errorStream = process.getErrorStream();
//        // Read commands and responses from here
//        final InputStream inputStream = process.getInputStream();
//        // Write commands and responses to here
//        final OutputStream outputStream = process.getOutputStream();
        
        String serverName = serverConfig.getServerName();
        List<String> command = getServerLaunchCommand(serverConfig);
        Map<String, String> env = getServerLaunchEnvironment(serverConfig.getJvm());
        processManagerSlave.addProcess(serverName, command, env, environment.getHomeDir().getAbsolutePath());
        processManagerSlave.startProcess(serverName);
        
        // TODO JBAS-8260 If serverConfig specified that server will work with
        // ServerManager over sockets, create a socket-based ServerCommunicationHandler
        ServerCommunicationHandler commHandler = new ProcessManagerServerCommunicationHandler(serverName, processManagerSlave);
        Server server = new Server(commHandler);
//        messageHandler.registerServer(serverConfig.getServerName(), server);
        return server;
    }

    private List<String> getServerLaunchCommand(Standalone serverConfig) {
        
        JvmElement jvm = serverConfig.getJvm();
        
        List<String> command = new ArrayList<String>();
        
//      if (false) {
//          // Example: run at high priority on *NIX
//          args.add("/usr/bin/nice");
//          args.add("-n");
//          args.add("-10");
//      }
//      if (false) {
//          // Example: run only on processors 1-4 on Linux
//          args.add("/usr/bin/taskset");
//          args.add("0x0000000F");
//      }
        
        command.add(getJavaCommand(jvm));
        
        Map<String, String> sysProps = appendJavaOptions(jvm, command);
        
        command.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        command.add("-jar");
        command.add("jboss-modules.jar");
        command.add("-mp");
        command.add("modules");
        command.add("-logmodule");
        command.add("org.jboss.logmanager:jboss-logmanager");
        command.add("org.jboss.as:jboss-as-server");
        
        appendArgsToMain(serverConfig, sysProps, command);
        
        return command;
    }

    private String getJavaCommand(JvmElement jvm) {
        String javaHome = jvm.getJavaHome();
        if (javaHome == null) { // TODO should this be possible?            
            return "java"; // hope for the best
        }
        
        File f = new File(javaHome);
        f = new File(f, "bin");
        f = new File (f, "java");
        return f.getAbsolutePath();
    }

    private Map<String, String> appendJavaOptions(JvmElement jvm, List<String> command) {
        
        String heap = jvm.getHeapSize();
        String max = jvm.getMaxHeap();
        
        // FIXME not the correct place to establish defaults
        if (max == null && heap != null) {
            max = heap;
        }
        if (heap == null && max != null) {
            heap = max;
        }
        
        if (heap != null) {
            command.add("-Xms"+ heap);
        }
        if (max != null) {
            command.add("-Xmx"+ max);
        }
        
        PropertiesElement propsEl = jvm.getSystemProperties();
        Map<String, String> sysProps = propsEl == null ? new HashMap<String, String>() : propsEl.getProperties();
        addStandardProperties(sysProps);
        
        for (Map.Entry<String, String> prop : sysProps.entrySet()) {
            StringBuilder sb = new StringBuilder("-D");
            sb.append(prop.getKey());
            sb.append('=');
            sb.append(prop.getValue() == null ? "true" : prop.getValue());
            command.add(sb.toString());
        }
        
        return sysProps;
    }

    private void appendArgsToMain(Standalone serverConfig, Map<String, String> jvmProps, List<String> command) {

        if (environment.getProcessManagerAddress() != null) {
            command.add("-interprocess-address");
            command.add(environment.getProcessManagerAddress().getHostAddress());
            command.add("-interprocess-port");
            command.add(environment.getProcessManagerPort().toString());
        }
        
        // Pass through as args to main any sys props that are read at primordial boot
        Map<String, String> sysProps = null;
        PropertiesElement propsEl = serverConfig.getSystemProperties();
        if (propsEl == null) {
            sysProps = Collections.emptyMap();
        }
        else {
            sysProps = propsEl.getProperties();
        }
        
        StringBuilder sb = new StringBuilder("-D");
        sb.append(ServerManagerEnvironment.HOME_DIR);
        sb.append('=');
        sb.append(getPropertyValue(ServerManagerEnvironment.HOME_DIR, jvmProps, sysProps, environment.getHomeDir().getAbsolutePath()));
        
        String key = "jboss.server.base.dir";  // TODO fragile! common constant between server-manager and server modules
        sb = new StringBuilder("-D");
        sb.append(key);
        sb.append('=');
        File serverBaseDir = new File(environment.getDomainServersDir(), serverConfig.getServerName());
        sb.append(getPropertyValue(key, jvmProps, sysProps, serverBaseDir.getAbsolutePath()));
        
        // TODO fragile! common constants between server-manager and server modules
        String[] keys = {"jboss.server.config.dir", "jboss.server.config.dir", 
                "jboss.server.data.dir", "jboss.server.log.dir", "jboss.server.temp.dir"};
        for (String propkey : keys)
        if (sysProps.containsKey(propkey)) {
            
            sb = new StringBuilder("-D");
            sb.append(propkey);
            sb.append('=');
            sb.append(sysProps.get(propkey));
        }
        
    }

    private static String getPropertyValue(String property, Map<String, String> jvmProps, Map<String, String> sysProps, String defaultVal) {
        String result = jvmProps.get(property);
        if (result == null) {
            result = sysProps.get(property);
        }
        return result == null ? defaultVal : result;
    }

    /**
     * Equivalent to default JAVA_OPTS in < AS 6 run.conf file
     * 
     * TODO externalize this somewhere if doing this at all is the right thing
     * 
     * @param sysProps
     */
    private void addStandardProperties(Map<String, String> sysProps) {
        // 
        if (!sysProps.containsKey("sun.rmi.dgc.client.gcInterval")) {
            sysProps.put("sun.rmi.dgc.client.gcInterval","3600000");
        }
        if (!sysProps.containsKey("sun.rmi.dgc.server.gcInterval")) {
            sysProps.put("sun.rmi.dgc.server.gcInterval","3600000");
        }
        
        // Following is disabled per:
        // Jason: not sure if it makes sense
        // Jason: everything should be doing Class.forName
        // David: agreed on that
        // Jason: and if they enable that option
        // Jason: it would do the equiv
        // Jason: so my vote is dont        
//        if (!sysProps.containsKey("sun.lang.ClassLoader.allowArraySyntax")) {
//            sysProps.put("sun.lang.ClassLoader.allowArraySyntax","true");
//        }
    }

    private Map<String, String> getServerLaunchEnvironment(JvmElement jvm) {
        Map<String, String> env = null;
        PropertiesElement pe = jvm.getEnvironmentVariables();
        if (pe != null) {
            env = pe.getProperties();
        }
        else {
            env = Collections.emptyMap();
        }
        return env;
    }
    
}
