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

package org.jboss.as.test.integration.domain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.junit.Test;

/**
 * Test validating the configuration starts and can accept a simple web request.
 *
 * @author Emanuel Muckenhuber
 */
public abstract class BuildConfigurationTestBase {

    static final String masterAddress = System.getProperty("jboss.test.host.master.address", "localhost");
    static final File CONFIG_DIR = new File("target/jbossas/domain/configuration/");

    @Test
    public void test() throws Exception {
        final JBossAsManagedConfiguration config = createConfiguration(getDomainConfigFile(), getHostConfigFile(), getClass().getSimpleName());
        final DomainLifecycleUtil utils = new DomainLifecycleUtil(config);
        utils.start(); // Start
        try {
            URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(masterAddress) + ":8080").openConnection();
            connection.connect();
        } finally {
            utils.stop(); // Stop
        }
    }

    protected abstract String getDomainConfigFile();

    protected abstract String getHostConfigFile();


    static JBossAsManagedConfiguration createConfiguration(final String domainXmlName, final String hostXmlName, final String testConfiguration) {
        final File output = new File("target" + File.separator + "domains" + File.separator + testConfiguration);
        final JBossAsManagedConfiguration configuration = new JBossAsManagedConfiguration();

        configuration.setHostControllerManagementAddress(masterAddress);
        configuration.setHostCommandLineProperties("-Djboss.test.host.master.address=" + masterAddress);
        configuration.setDomainConfigFile(hackReplaceProperty(new File(CONFIG_DIR, domainXmlName)).getAbsolutePath());
        configuration.setHostConfigFile(hackReplaceInterfaces(new File(CONFIG_DIR, hostXmlName)).getAbsolutePath());

        configuration.setHostName("master"); // TODO this shouldn't be needed

        new File(output, "configuration").mkdirs(); // TODO this should not be necessary
        configuration.setDomainDirectory(output.getAbsolutePath());

        System.out.println(configuration.getDomainConfigFile());
        System.out.println(configuration.getHostConfigFile());
        return configuration;

    }

    //HACK to make the interfaces settable - I could not do it in xsl since it was replacing the system property
    static File hackReplaceInterfaces(File hostConfigFile) {
        final File file;
        final BufferedWriter writer;
        try {
            file = File.createTempFile("host", ".xml", hostConfigFile.getAbsoluteFile().getParentFile());
            file.deleteOnExit();
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(hostConfigFile));
            try {
                String line = reader.readLine();
                while (line != null) {
                    int start = line.indexOf("<inet-address value=\"");
                    if (start >= 0) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(line.substring(0, start));
                        sb.append("<inet-address value=\"" + masterAddress + "\"/>");
                        writer.write(sb.toString());
                    } else {
                        start = line.indexOf("<option value=\"");
                        if (start >= 0) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(line.substring(0, start));
                            List<String> opts = new ArrayList<String>();
                            TestSuiteEnvironment.getIpv6Args(opts);
                            for (String opt : opts) {
                                sb.append("<option value=\"" + opt + "\"/>");
                            }

                            writer.write(sb.toString());
                        } else {
                            start = line.indexOf("java.net.preferIPv4Stack");
                            if (start < 0) {
                                writer.write(line);
                            }
                        }
                    }
                    writer.write("\n");
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                safeClose(reader);
                safeClose(writer);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    //HACK to make the interfaces settable - I could not do it in xsl since it was replacing the system property
    static File hackReplaceProperty(File hostConfigFile) {
        final File file;
        final BufferedWriter writer;
        try {
            file = File.createTempFile("domain", ".xml", hostConfigFile.getAbsoluteFile().getParentFile());
            file.deleteOnExit();
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(hostConfigFile));
            try {
                String line = reader.readLine();
                while (line != null) {
                    int start = line.indexOf("java.net.preferIPv4Stack");
                    if (start < 0) {
                        writer.write(line);
                    }
                    writer.write("\n");
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                safeClose(reader);
                safeClose(writer);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    static void safeClose(Closeable c) {
        try {
            c.close();
        } catch (Exception ignore) {
        }
    }
}
