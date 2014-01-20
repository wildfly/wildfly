package org.jboss.as.arquillian.protocol.jmx;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

/**
 * Massive hack that attempts to improve test suite reliability by killing servers between runs
 * <p/>
 * This means that if a server fails to stop it will not affect later runs
 *
 * @author Stuart Douglas
 */
public class ServerKillerExtension {

    public static final String ORG_WILDFLY_TEST_KILL_SERVERS_BEFORE_TEST = "org.wildfly.test.kill-servers-before-test";

    private static volatile String errorProcessTable;

    private static boolean runOncePerSuite = true;

    private String javaHome;

    public ServerKillerExtension() {
        javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }
    }

    public void beforeSuite(@Observes BeforeSetup start) {
        if (runOncePerSuite) {
            killServers();
        }
    }

    public void killServers() {
        if (!Boolean.getBoolean(ORG_WILDFLY_TEST_KILL_SERVERS_BEFORE_TEST)) {
            return;
        }
        runOncePerSuite = false;

        //server is running and port is open.
        Runtime rt = Runtime.getRuntime();
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            killWindows(rt);
        } else {
            killLinux(rt);
        }
    }

    public void throwErrpr(@Observes AfterSuite start) {
        runOncePerSuite = true;
        String result = errorProcessTable;
        errorProcessTable = null;
        if (result != null) {
            throw new RuntimeException("There was a server running at the start of the test run execution. jstack output was \n" + result);
        }
    }

    private String getJStackPath() {
        return javaHome + File.separator + "bin" + File.separator + "jstack";
    }

    private String getJps() {
        return javaHome + File.separator + "bin" + File.separator + "jps";
    }


    private void killLinux(Runtime rt) {
        try {

            //get a jstack of all the processes
            Process process = rt.exec(new String[]{"/bin/sh", "-c", getJps() + " | egrep -v \"Jps|AgentMain|Launcher|RemoteMavenServer\" | awk '{ print $1; }' | xargs --no-run-if-empty " + getJStackPath()});
            InputStream in = process.getInputStream();
            String processTable = readString(in);
            readString(process.getErrorStream());
            if (!processTable.isEmpty()) {
                readString(rt.exec(new String[]{"/bin/sh", "-c", getJps() + " | egrep -v \"Jps|AgentMain|Launcher|RemoteMavenServer\" | awk '{ print $1; }' | xargs --no-run-if-empty kill -9"}).getInputStream());
                errorProcessTable = processTable;
            }
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end) {
                String running = readString(rt.exec(new String[]{"/bin/sh", "-c", getJps() + " | egrep -v \"Jps|AgentMain|Launcher|RemoteMavenServer\" | awk '{ print $1; }' | xargs --no-run-if-empty " + getJStackPath()}).getInputStream());
                if (running.isEmpty()) {
                    break;
                } else {
                    Thread.sleep(100);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void killWindows(Runtime rt) {
        final String GET_PROCESSES_COMMAND = "gwmi win32_process -filter \"name='java.exe' and commandLine like '%jboss-modules%' \" | foreach { " + getJStackPath() + " $_.ProcessId }";
        final String KILL_PROCESSES_COMMAND = "gwmi win32_process -filter \"name='java.exe' and commandLine like '%jboss-modules%' \" | foreach { kill -id $_.ProcessId }";

        try {
            Path getProcessCommand = createCommandFile(GET_PROCESSES_COMMAND);
            Process process = rt.exec(new String[]{"powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "ByPass", getProcessCommand.toString()});


            InputStream in = process.getInputStream();
            String processTable = readString(in);
            String errors = readString(process.getErrorStream());
            if (errors != null && !errors.isEmpty()) {
                System.out.println("Could not get processes:\n" + errors);
            }

            if (!processTable.isEmpty()) {
                Path killProcessesCommand = createCommandFile(KILL_PROCESSES_COMMAND);
                readString(rt.exec(new String[]{"powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "ByPass", killProcessesCommand.toString()}).getInputStream());
                errorProcessTable = processTable;
                Files.delete(killProcessesCommand);
            }
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end) {
                String running = readString(rt.exec(new String[]{"powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "ByPass", getProcessCommand.toString()}).getInputStream());
                if (running.isEmpty()) {
                    break;
                } else {
                    Thread.sleep(100);
                }
            }
            Files.delete(getProcessCommand);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
        we save command into powershell script as otherwise there is too much issues with escaping
         */
    private static Path createCommandFile(String command) throws IOException {
        Path tmp = Files.createTempFile("pskiller", ".ps1");
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmp.toFile()))) {
            bos.write(command.getBytes());
        }
        tmp.toFile().deleteOnExit();
        return tmp;
    }

    public static String readString(InputStream file) {
        BufferedInputStream stream = null;
        try {
            stream = new BufferedInputStream(file);
            byte[] buff = new byte[1024];
            StringBuilder builder = new StringBuilder();
            int read = -1;
            while ((read = stream.read(buff)) != -1) {
                builder.append(new String(buff, 0, read));
            }
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

}
