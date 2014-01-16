package org.jboss.as.arquillian.protocol.jmx;

import org.jboss.arquillian.container.spi.event.container.BeforeSetup;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * Massive hack that attempts to improve test suite reliability by killing servers between runs
 *
 * This means that if a server fails to stop it will not affect later runs
 *
 *
 * @author Stuart Douglas
 */
public class ServerKillerExtension {

    public static final String ORG_WILDFLY_TEST_KILL_SERVERS_BEFORE_TEST = "org.wildfly.test.kill-servers-before-test";

    private static volatile String errorProcessTable;

    private static boolean runOncePerSuite = true;

    public void beforeSuite(@Observes BeforeSetup start) {
        if(runOncePerSuite) {
            killServers();
        }
    }

    public void killServers() {
        if(!Boolean.getBoolean(ORG_WILDFLY_TEST_KILL_SERVERS_BEFORE_TEST)) {
            return;
        }
        runOncePerSuite = false;

        //server is running and port is open.
        Runtime rt = Runtime.getRuntime();
        if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            //rt.exec("taskkill " +....); //windows not supported yet
        } else {
            killLinux(rt);
        }
    }

    public void throwErrpr(@Observes AfterSuite start) {
        runOncePerSuite = true;
        String result = errorProcessTable;
        errorProcessTable = null;
        if(result != null) {
            throw new RuntimeException("There was a server running at the start of the test run execution. jstack output was \n" + result);
        }
    }


    private void killLinux(Runtime rt) {
        try {

            //get a jstack of all the processes
            Process process = rt.exec(new String[]{"/bin/sh", "-c", "ps -eaf --columns 20000 | grep org.jboss.as | grep jboss-modules.jar | grep -v -w grep | awk '{ print $2; }' | xargs --no-run-if-empty jstack"});
            InputStream in = process.getInputStream();
            String processTable = readString(in);
            if(!processTable.isEmpty()) {
                readString(rt.exec(new String[]{"/bin/sh", "-c", "ps -eaf --columns 20000 | grep org.jboss.as | grep jboss-modules.jar | grep -v -w grep | awk '{ print $2; }' | xargs --no-run-if-empty kill -9"}).getInputStream());
                errorProcessTable = processTable;
            }
            long end = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < end) {
                String running = readString(rt.exec(new String[]{"/bin/sh", "-c", "ps -eaf --columns 20000 | grep org.jboss.as | grep jboss-modules.jar | grep -v -w grep | awk '{ print $2; }'"}).getInputStream());
                if(running.isEmpty()) {
                    break;
                } else {
                    Thread.sleep(100);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
