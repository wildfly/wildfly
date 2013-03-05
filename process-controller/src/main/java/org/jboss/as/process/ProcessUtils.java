package org.jboss.as.process;

import org.jboss.as.process.protocol.StreamUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Process utilities.
 */
abstract class ProcessUtils {

    private static final ProcessUtils processUtils;
    private static final String modulesJar = "jboss-modules.jar";

    static {
        if (File.pathSeparatorChar == ':'){
            processUtils = new UnixProcessUtils();
        } else {
            processUtils = new WindowsProcessUtils();
        }
    }

    /**
     * Try to kill a given process.
     *
     * @param processName the process name
     * @return {@code true} if the command succeeded, {@code false} otherwise
     */
    static boolean killProcess(final String processName) {
        int pid;
        try {
            pid = processUtils.resolveProcessId(processName);
            if(pid > 0) {
                try {
                    Runtime.getRuntime().exec(processUtils.getKillCommand(pid));
                    return true;
                } catch (Throwable t) {
                    ProcessLogger.ROOT_LOGGER.debugf(t, "failed to kill process '%s' with pid '%s'", processName, pid);
                }
            }
        } catch (Throwable t) {
            ProcessLogger.ROOT_LOGGER.debugf(t, "failed to resolve pid of process '%s'", processName);
        }
        return false;
    }

    /**
     * Resolve the java home.
     *
     * @return the java home
     */
    protected String getJavaHome() {
        return SecurityActions.getSystemProperty("java.home", ".");
    }

    /**
     * Get the jps command.
     *
     * @return the jps command, {@code null} if not found
     */
    abstract String getJpsCommand();

    /**
     * Get the kill command.
     *
     * @param processId the process id to kill
     * @return the kill command
     */
    abstract String getKillCommand(int processId);

    /**
     * Iterate through all java processes and try to find the one matching to the given process id.
     * This will return the resolved process-id or {@code -1} if not resolvable.
     *
     * @param processName the process name
     * @return the process id
     * @throws IOException
     */
    private int resolveProcessId(final String processName) throws IOException {
        final String jpsCommand = getJpsCommand();
        if(jpsCommand == null) {
            ProcessLogger.ROOT_LOGGER.debugf("jps command not found'");
            return -1;
        }
        final Process p = Runtime.getRuntime().exec(jpsCommand);
        final List<String> processes = new ArrayList<>();
        final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String line;
            // See if the process contains "jboss-modules.jar" and "-D[Server:server-one]"
            final String process = "-D[" + processName + "]";
            while ((line = input.readLine()) != null) {
                if (line.contains(modulesJar) && line.contains(process)){
                    processes.add(line);
                }
            }
        } finally {
            StreamUtils.safeClose(input);
        }
        if(processes.size() == 1) {
            final String proc = processes.get(0);
            final int i = proc.indexOf(' ');
            return Integer.parseInt(proc.substring(0, i));
        }
        if(processes.isEmpty()) {
            ProcessLogger.ROOT_LOGGER.debugf("process not found '%s'", processName);
        } else {
            ProcessLogger.ROOT_LOGGER.debugf("ambiguous result. multiple processes available for '%s'", processName);
        }
        return -1;
    }

    private static class UnixProcessUtils extends ProcessUtils {
        @Override
        String getJpsCommand() {
            final File jreHome = new File(getJavaHome());
            File jpsExe = new File(jreHome, "bin/jps");
            if (!jpsExe.exists()) {
                jpsExe = new File(jreHome, "../bin/jps");
            }
            if(jpsExe.exists()) {
                return String.format("%s -vl", jpsExe.getAbsolutePath());
            }
            return null;
        }

        @Override
        String getKillCommand(int process) {
            return "kill -9 " + process;
        }
    }

    private static class WindowsProcessUtils extends ProcessUtils {

        @Override
        String getJpsCommand() {
            final File jreHome = new File(getJavaHome());
            File jpsExe = new File(jreHome, "bin/jps.exe");
            if (!jpsExe.exists()) {
                jpsExe = new File(jreHome, "../bin/jps.exe");
            }
            if(jpsExe.exists()) {
                return String.format("%s -vl", jpsExe.getAbsolutePath());
            }
            return null;
        }

        @Override
        String getKillCommand(int process) {
            return "taskkill /f /pid " + process;
        }
    }

}
