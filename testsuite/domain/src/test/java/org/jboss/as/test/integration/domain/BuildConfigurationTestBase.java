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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

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

import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfiguration;
import org.jboss.as.test.integration.domain.management.util.JBossAsManagedConfigurationParameters;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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

            if (Boolean.getBoolean("expression.audit")) {
                writeExpressionAudit(utils);
            }
        } finally {
            utils.stop(); // Stop
        }
    }

    protected abstract String getDomainConfigFile();

    protected abstract String getHostConfigFile();


    static JBossAsManagedConfiguration createConfiguration(final String domainXmlName, final String hostXmlName, final String testConfiguration) {
        final File output = new File("target" + File.separator + "domains" + File.separator + testConfiguration);
        final JBossAsManagedConfiguration configuration = new JBossAsManagedConfiguration(JBossAsManagedConfigurationParameters.STANDARD);

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

    private void writeExpressionAudit(final DomainLifecycleUtil utils) throws IOException {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(RECURSIVE).set(true);

        final ModelNode result = utils.getDomainClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        Assert.assertTrue(result.hasDefined(RESULT));

        PathAddress pa = PathAddress.EMPTY_ADDRESS;
        writeExpressionAudit(pa, result.get(RESULT));
    }

    private static void writeExpressionAudit(PathAddress pa, ModelNode resourceDescription) {
        String paString = getPaString(pa);
        if (resourceDescription.hasDefined(ModelDescriptionConstants.ATTRIBUTES)) {
            for (Property property : resourceDescription.get(ModelDescriptionConstants.ATTRIBUTES).asPropertyList()) {
                ModelNode attrdesc = property.getValue();
                if (!attrdesc.hasDefined(ModelDescriptionConstants.STORAGE) ||
                        AttributeAccess.Storage.CONFIGURATION.name().toLowerCase().equals(attrdesc.get(ModelDescriptionConstants.STORAGE).asString().toLowerCase())) {
                    StringBuilder sb = new StringBuilder(paString);
                    sb.append(",").append(property.getName());
                    sb.append(",").append(attrdesc.get(ModelDescriptionConstants.TYPE).asString());
                    sb.append(",").append(attrdesc.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).asBoolean(false));
                    sb.append(",").append(attrdesc.get(ModelDescriptionConstants.DESCRIPTION).asString());
                    System.out.println(sb.toString());
                }
            }
        }

        if (resourceDescription.hasDefined(ModelDescriptionConstants.CHILDREN)) {
            for (Property childTypeProp : resourceDescription.get(ModelDescriptionConstants.CHILDREN).asPropertyList()) {
                String childType = childTypeProp.getName();
                ModelNode childTypeDesc = childTypeProp.getValue();
                if (childTypeDesc.hasDefined(ModelDescriptionConstants.MODEL_DESCRIPTION)) {
                    for (Property childInstanceProp : childTypeDesc.get(ModelDescriptionConstants.MODEL_DESCRIPTION).asPropertyList()) {
                        PathAddress childAddress = pa.append(childType, childInstanceProp.getName());
                        writeExpressionAudit(childAddress, childInstanceProp.getValue());
                    }
                }
            }
        }

    }

    private static String getPaString(PathAddress pa) {
        if (pa.size() == 0) {
            return "/";
        }
        StringBuilder sb = new StringBuilder();
        for (PathElement pe : pa) {
            sb.append("/").append(pe.getKey()).append("=").append(pe.getValue());
        }
        return sb.toString();
    }
}
