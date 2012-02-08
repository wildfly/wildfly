/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.Assert;

import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigurationAssemblerTestCase {

    @Test
    public void testStandaloneConfigurationAssembly() throws Exception {
        File baseDir = getFileForResource("standalone-template.xml").getAbsoluteFile().getParentFile();
        File outputFile = new File(new File(".").getParentFile(), "target/output/standalone.xml");
        ConfigurationAssembler assembler = new ConfigurationAssembler(
                baseDir,
                getFileForResource("standalone-template.xml"),
                "server",
                getFileForResource("subsystems.xml"),
                outputFile);
        assembler.assemble();

        String outputXml = readOutputFile(outputFile);
    }

    @Test
    public void testDomainAssembly() throws Exception {
        File baseDir = getFileForResource("domain-template.xml").getAbsoluteFile().getParentFile();
        File outputFile = new File(new File(".").getParentFile(), "target/output/domain.xml");

        ConfigurationAssembler assembler = new ConfigurationAssembler(
                baseDir,
                getFileForResource("domain-template.xml"),
                "server",
                getFileForResource("subsystems.xml"),
                outputFile);
        assembler.assemble();

        String outputXml = readOutputFile(outputFile);
    }

    @Test
    public void testStandalone2ConfigurationAssembly() throws Exception {
        File baseDir = getFileForResource("standalone-template.xml").getAbsoluteFile().getParentFile();
        File outputFile = new File(new File(".").getParentFile(), "target/output/standalone2.xml");
        ConfigurationAssembler assembler = new ConfigurationAssembler(
                baseDir,
                getFileForResource("standalone-template.xml"),
                "server",
                getFileForResource("subsystems2.xml"),
                outputFile);
        assembler.assemble();

        String outputXml = readOutputFile(outputFile);

    }

    @Test
    public void testEmptyStandaloneConfigurationAssembly() throws Exception {
        File baseDir = getFileForResource("standalone-template.xml").getAbsoluteFile().getParentFile();
        File outputFile = new File(new File(".").getParentFile(), "target/output/standalone2.xml");
        ConfigurationAssembler assembler = new ConfigurationAssembler(
                baseDir,
                getFileForResource("standalone-template.xml"),
                "server",
                getFileForResource("subsystems-empty.xml"),
                outputFile);
        assembler.assemble();

        String outputXml = readOutputFile(outputFile);
        Assert.assertFalse(outputXml.contains("extensions"));
        Assert.assertFalse(outputXml.contains("profile"));

    }


    private File getFileForResource(String name) throws MalformedURLException, URISyntaxException {
        URL url = this.getClass().getResource(name);
        return new File(url.toURI());
    }

    private String readOutputFile(File outputFile) throws IOException, FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader(outputFile));
        StringWriter output = new StringWriter();
        BufferedWriter writer = new BufferedWriter(output);
        try {
            String line = reader.readLine();
            while (line != null) {
                writer.write(line);
                writer.write("\n");
                line = reader.readLine();
            }
        } finally {
            try {
                reader.close();
            } catch (Exception ignore) {
            }
            try {
                writer.close();
            } catch (Exception ignore) {
            }
        }
        return output.toString();
    }
}
