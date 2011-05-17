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
package org.jboss.as.test.domain;

import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.process.Main;
import org.jboss.as.process.ProcessController;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * RespawnTestCase
 *
 * TODO move into integration once working properly
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Ignore("Test migrated to managed container")
public class RespawnTestCase {

    private static final int TIMEOUT = 15000;
    private static final String HOST_CONTROLLER = "host-controller";
    private static final String PROCESS_CONTROLLER = "process";
    private static final String SERVER_ONE = "server-one";
    private static final String SERVER_TWO = "server-two";
    private static final int HC_PORT = 9999;


    static ProcessController processController;
    static ProcessUtil processUtil;

    @BeforeClass
    public static void createProcessController() throws IOException {
        if (File.pathSeparatorChar == ':'){
            processUtil = new UnixProcessUtil();
        } else {
            processUtil = new WindowsProcessUtil();
        }

        String jbossHome = System.getProperty("jboss.home");
        System.out.println("---- " + jbossHome);
        List<String> args = new ArrayList<String>();
        args.add("-jboss-home");
        args.add(jbossHome);
        args.add("-jvm");
        args.add(processUtil.getJavaCommand());
        args.add("--");
        args.add("-Dorg.jboss.boot.log.file=" + jbossHome + "/domain/log/host-controller/boot.log");
        args.add("-Dlogging.configuration=file:" + jbossHome + "/domain/configuration/logging.properties");
        args.add("-Xms64m");
        args.add("-Xmx512m");
        args.add("-XX:MaxPermSize=256m");
        args.add("-Dorg.jboss.resolver.warning=true");
        args.add("-Dsun.rmi.dgc.client.gcInterval=3600000");
        args.add("-Dsun.rmi.dgc.server.gcInterval=3600000");
        args.add("--");
        args.add("-default-jvm");
        args.add(processUtil.getJavaCommand());

        processController = Main.start(args.toArray(new String[args.size()]));
    }

    @AfterClass
    public static void destroyProcessController(){
        if (processController != null){
            processController.shutdown();
            processController = null;
        }
    }

    @Test
    public void testDomainRespawn() throws Exception {
        //Make sure everything started
        List<RunningProcess> processes = waitForAllProcesses();
        readHostControllerServers();

        //Kill the HC and make sure that it gets restarted
        RunningProcess originalHc = processUtil.getProcess(processes, HOST_CONTROLLER);
        Assert.assertNotNull(originalHc);
        System.out.println("!!!!!!!!!! KILLING");
        processUtil.killProcess(originalHc);
        processes = waitForAllProcesses();
        RunningProcess respawnedHc = processUtil.getProcess(processes, HOST_CONTROLLER);
        Assert.assertNotNull(respawnedHc);
        Assert.assertFalse(originalHc.getProcessId().equals(respawnedHc.getProcessId()));


        //Hack - protocol hangs if the remote message handler does not exist yet
        Thread.sleep(5000);

        readHostControllerServers();

        //Kill a server and make sure that it gets restarted
        RunningProcess originalServerOne = processUtil.getProcess(processes, SERVER_ONE);
        Assert.assertNotNull(originalServerOne);
        processUtil.killProcess(originalServerOne);
        processes = waitForAllProcesses();
        RunningProcess respawnedServerOne = processUtil.getProcess(processes, SERVER_ONE);
        Assert.assertNotNull(respawnedServerOne);
        Assert.assertFalse(originalServerOne.getProcessId().equals(respawnedServerOne.getProcessId()));
        readHostControllerServers();
    }


    private void readHostControllerServers() throws Exception {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        operation.get(ModelDescriptionConstants.OP_ADDR).set(PathAddress.pathAddress(PathElement.pathElement("host", "local")).toModelNode());
        operation.get(ModelDescriptionConstants.RECURSIVE).set(true);

        final long time = System.currentTimeMillis() + TIMEOUT;
        boolean hasOne = false;
        boolean hasTwo = false;
        do {
            final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getLocalHost(), HC_PORT);
            Thread hack = new Thread(new Runnable() {
               @Override
               public void run() {
                   try {
                       Thread.sleep(1000);
                   } catch (InterruptedException e) {
                   }
                   StreamUtils.safeClose(client);
               }
           });
           hack.start();
            try {
                final ModelNode result = client.execute(operation);
                if (result.get(ModelDescriptionConstants.OUTCOME).asString().equals(ModelDescriptionConstants.SUCCESS)){
                    final ModelNode model = result.require(ModelDescriptionConstants.RESULT);
                    hasOne = model.get(ModelDescriptionConstants.HOST, ModelDescriptionConstants.LOCAL, ModelDescriptionConstants.RUNNING_SERVER).hasDefined(SERVER_ONE);
                    hasTwo = model.get(ModelDescriptionConstants.HOST, ModelDescriptionConstants.LOCAL, ModelDescriptionConstants.RUNNING_SERVER).hasDefined(SERVER_TWO);
                    if (hasOne && hasTwo){
                        return;
                    }
                }
            } catch (IOException e) {
            } finally {
                hack.interrupt();
                StreamUtils.safeClose(client);
            }

            Thread.sleep(200);
        } while (System.currentTimeMillis() < time);
        Assert.assertTrue(hasOne);
        Assert.assertTrue(hasTwo);
    }

    private List<RunningProcess> waitForAllProcesses() throws Exception {
        final long time = System.currentTimeMillis() + TIMEOUT;
        List<RunningProcess> runningProcesses;
        do {
            runningProcesses = processUtil.getRunningProcesses();
            if (processUtil.containsProcesses(runningProcesses, HOST_CONTROLLER, SERVER_ONE, SERVER_TWO)){
                return runningProcesses;
            }
            Thread.sleep(200);
        } while(System.currentTimeMillis() < time);
        Assert.fail("Did not have all running processes " + runningProcesses);
        return null;
    }

    private static abstract class ProcessUtil {

        List<String> initialProcessIds;

        ProcessUtil(){
            initialProcessIds = getInitialProcessIds();
        }

        List<String> getInitialProcessIds(){
            List<String> processes = listProcesses();
            List<String> ids = new ArrayList<String>();
            for (String proc : processes){
                ids.add(parseProcessId(proc));
            }
            return ids;
        }

        String parseProcessId(String proc){
            int i = proc.indexOf(' ');
            return proc.substring(0, i);
        }

        List<RunningProcess> getRunningProcesses(){
            List<RunningProcess> running = new ArrayList<RunningProcess>();
            List<String> processes = listProcesses();
            for (String proc : processes){
                String id = parseProcessId(proc);
                if (!initialProcessIds.contains(id)){
                    if (proc.contains(HOST_CONTROLLER) && !proc.contains(PROCESS_CONTROLLER)){
                        running.add(new RunningProcess(id, HOST_CONTROLLER));
                    } else if (proc.contains(SERVER_ONE)){
                        running.add(new RunningProcess(id, SERVER_ONE));
                    } else if (proc.contains(SERVER_TWO)){
                        running.add(new RunningProcess(id, SERVER_TWO));
                    }
                }
            }
            return running;
        }

        List<String> listProcesses() {
            final Process p;
            try {
                p = Runtime.getRuntime().exec(getJpsCommand());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<String> processes = new ArrayList<String>();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.contains("jboss-modules.jar")){
                        processes.add(line);
                    }

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                StreamUtils.safeClose(input);
            }
            return processes;
        }

        void killProcess(RunningProcess process) {
            try {
                Runtime.getRuntime().exec(getKillCommand(process));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final long time = System.currentTimeMillis() + TIMEOUT;
            do {
                List<RunningProcess> runningProcesses = processUtil.getRunningProcesses();
                if (processUtil.getProcessById(runningProcesses, process.getProcessId()) == null){
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while(System.currentTimeMillis() < time);

            Assert.fail("Did not kill process " + process + " " + processUtil.getRunningProcesses());
        }

        abstract String getJpsCommand();

        abstract String getJavaCommand();

        abstract String getKillCommand(RunningProcess process);

        private boolean containsProcesses(List<RunningProcess> runningProcesses, String...names){
            for (String name : names){
                boolean found = false;
                for (RunningProcess proc : runningProcesses) {
                    if (proc.getProcess().equals(name)){
                        found = true;
                        continue;
                    }
                }
                if (!found){
                    return false;
                }
            }
            return true;
        }

        private RunningProcess getProcess(List<RunningProcess> runningProcesses, String name){
            for (RunningProcess proc : runningProcesses){
                if (proc.getProcess().equals(name)){
                    return proc;
                }
            }
            return null;
        }

        private RunningProcess getProcessById(List<RunningProcess> runningProcesses, String id){
            for (RunningProcess proc : runningProcesses){
                if (proc.getProcessId().equals(id)){
                    return proc;
                }
            }
            return null;
        }
    }

    private static class UnixProcessUtil extends ProcessUtil {
        @Override
        String getJpsCommand() {
            return System.getProperty("java.home") + "/bin/jps -vl";

        }

        @Override
        String getJavaCommand() {
            return System.getProperty("java.home") + "/bin/java";
        }

        @Override
        String getKillCommand(RunningProcess process) {
            return "kill -9 " + process.getProcessId();
        }
    }

    private static class WindowsProcessUtil extends ProcessUtil {

        @Override
        String getJpsCommand() {
            return System.getProperty("java.home") + "/bin/jps.exe -vl";
        }

        @Override
        String getJavaCommand() {
            return System.getProperty("java.home") + "/bin/java.exe";
        }

        @Override
        String getKillCommand(RunningProcess process) {
            return "taskkill /pid " + process.getProcessId();
        }
    }

    private static class RunningProcess {
        final String processId;
        final String process;

        private RunningProcess(String processId, String process) {
            this.processId = processId;
            this.process = process;
        }

        public String getProcessId() {
            return processId;
        }

        public String getProcess() {
            return process;
        }

        @Override
        public String toString() {
            return "Process{id=" + processId + ", process=" + process + "}";
        }
    }

}
