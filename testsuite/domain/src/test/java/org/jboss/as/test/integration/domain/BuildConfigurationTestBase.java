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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.test.integration.domain.management.util.WildFlyManagedConfiguration;
import org.jboss.as.test.shared.TestSuiteEnvironment;

/**
 * Base class for tests that use the standard AS configuration files.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public abstract class BuildConfigurationTestBase {

    static final String masterAddress = System.getProperty("jboss.test.host.master.address", "localhost");

    static final File CONFIG_DIR = new File("target/wildfly/domain/configuration/");

    static WildFlyManagedConfiguration createConfiguration(final String domainXmlName, final String hostXmlName, final String testConfiguration) {
        return createConfiguration(domainXmlName, hostXmlName, testConfiguration, "master", masterAddress, 9990);
    }

    static WildFlyManagedConfiguration createConfiguration(final String domainXmlName, final String hostXmlName,
                                                           final String testConfiguration, final String hostName,
                                                           final String hostAddress, final int hostPort) {
        final WildFlyManagedConfiguration configuration = new WildFlyManagedConfiguration();

        configuration.setHostControllerManagementAddress(hostAddress);
        configuration.setHostControllerManagementPort(hostPort);
        configuration.setHostControllerManagementProtocol("remote+http");
        configuration.setHostCommandLineProperties("-Djboss.domain.master.address=" + masterAddress +
                " -Djboss.management.http.port=" + hostPort);
        configuration.setDomainConfigFile(hackFixDomainConfig(new File(CONFIG_DIR, domainXmlName)).getAbsolutePath());
        configuration.setHostConfigFile(hackFixHostConfig(new File(CONFIG_DIR, hostXmlName), hostName, hostAddress).getAbsolutePath());
        //configuration.setHostConfigFile(new File(CONFIG_DIR, hostXmlName).getAbsolutePath());

        configuration.setHostName(hostName); // TODO this shouldn't be needed

        final File output = new File("target" + File.separator + "domains" + File.separator + testConfiguration + File.separator + hostName);
        new File(output, "configuration").mkdirs(); // TODO this should not be necessary
        configuration.setDomainDirectory(output.getAbsolutePath());

        return configuration;

    }

    private static File hackFixHostConfig(File hostConfigFile, String hostName, String hostAddress) {
        final Path file;
        final BufferedWriter writer;
        try {
            file = Files.createTempFile(hostConfigFile.toPath().getParent(),"host", ".xml");
            writer = Files.newBufferedWriter(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(hostConfigFile));
            try {
                String line = reader.readLine();
                boolean processedOpt = false;
                while (line != null) {
                    int start = line.indexOf("<host");
                    if (start >= 0 && !line.contains(" name=")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<host name=\"");
                        sb.append(hostName);
                        sb.append('"');
                        sb.append(line.substring(start + 5));
                        writer.write(sb.toString());
                    } else {
                        start = line.indexOf("<inet-address value=\"");
                        if (start >= 0) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(line.substring(0, start))
                                    .append("<inet-address value=\"")
                                    .append(hostAddress)
                                    .append("\"/>");
                            writer.write(sb.toString());
                        } else {
                            start = line.indexOf("<option value=\"");
                            if (start >= 0 && !processedOpt) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(line.substring(0, start));
                                List<String> opts = new ArrayList<String>();
                                TestSuiteEnvironment.getIpv6Args(opts);
                                for (String opt : opts) {
                                    sb.append("<option value=\"")
                                            .append(opt)
                                            .append("\"/>");
                                }

                                writer.write(sb.toString());
                                processedOpt = true;
                            } else if (!line.contains("java.net.")) {
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
        return file.toFile();
    }

    private static File hackFixDomainConfig(File domainConfigFile) {
        final File file;

        try {
            file = File.createTempFile("domain", ".xml", domainConfigFile.getAbsoluteFile().getParentFile());
            file.deleteOnExit();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(domainConfigFile));
                BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
            String line = reader.readLine();
            while (line != null) {
                if (line.contains("<security-setting name=\"#\">")) { //super duper hackish, just IO optimization
                    writer.write("        <journal type=\"NIO\" file-size=\"1024\" />");
                    writer.newLine();
                }

                int start = line.indexOf("java.net.preferIPv4Stack");
                if (start < 0) {
                    writer.write(line);
                }
                writer.newLine();
                line = reader.readLine();
            }
        } catch (IOException e) {
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
