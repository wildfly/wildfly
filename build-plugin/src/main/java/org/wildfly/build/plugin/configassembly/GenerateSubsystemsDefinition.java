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
package org.wildfly.build.plugin.configassembly;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Generate subsystems definition from a given spec
 *
 * @author Thomas.Diesler@jboss.com
 * @since 06-Sep-2012
 */
public class GenerateSubsystemsDefinition {

    private final List<SubsystemConfig> configs;
    private final String[] profiles;
    private final String filePrefix;
    private final File outputFile;

    /**
     * arg[0] - subsystems definition spec (e.g logging:osgi,osgi:eager,deployment-scanner)
     * arg[1] - subsytem profiles (e.g. default,ha,full,full-ha)
     * arg[2] - subsystem path prefix (e.g. configuration/subsystems)
     * arg[4] - the output file (e.g. domain-subsystems.xml)
     */
    public static void main(String[] args) throws Exception{
        if (args == null)
            throw new IllegalArgumentException("Null args");
        if (args.length < 4)
            throw new IllegalArgumentException("Invalid args: " + Arrays.asList(args));

        // spec := subsystem:supplement
        // definitions := definitions,spec

        int index = 0;
        if (args[index] == null || args[index].isEmpty()) {
            throw new IllegalArgumentException("No configured subsystems");
        }
        String definitions = args[index++];

        String[] profiles = new String[]{""};
        if (args[index] != null && !args[index].isEmpty()) {
            profiles = args[index].split(",");
        }
        index++;

        if (args[index] == null || args[index].isEmpty()) {
            throw new IllegalArgumentException("No file prefix");
        }
        String filePrefix = args[index++];
        if (!filePrefix.endsWith("/"))
            filePrefix += "/";

        if (args[index] == null || args[index].isEmpty()) {
            throw new IllegalArgumentException("No output file");
        }
        File outputFile = new File(args[index++]);

        List<SubsystemConfig> configs = new ArrayList<SubsystemConfig>();
        for (String spec : definitions.split(",")) {
            String[] split = spec.split(":");
            String subsystem = split[0];
            String supplement = split.length > 1 ? split[1] : null;
            configs.add(new SubsystemConfig(subsystem, supplement));
        }
        new GenerateSubsystemsDefinition(configs, profiles, filePrefix, outputFile).process();
    }

    private GenerateSubsystemsDefinition(List<SubsystemConfig> configs, String[] profiles, String filePrefix, File outputFile) {
        this.configs = configs;
        this.profiles = profiles;
        this.filePrefix = filePrefix;
        this.outputFile = outputFile;
    }

    private void process() throws XMLStreamException, IOException {

        ElementNode config = new ElementNode(null, "config", SubsystemsParser.NAMESPACE);
        for (String profile : profiles) {
            ElementNode subsystems = new ElementNode(config, "subsystems");
            if (!profile.isEmpty()) {
                subsystems.addAttribute("name", new AttributeValue(profile));
            }
            config.addChild(subsystems);

            for (SubsystemConfig sub : configs) {
                ElementNode subsystem = new ElementNode(config, "subsystem");
                if (sub.getSupplement() != null) {
                    subsystem.addAttribute("supplement", new AttributeValue(sub.getSupplement()));
                }
                subsystem.addChild(new TextNode(filePrefix + sub.getSubsystem() + ".xml"));
                subsystems.addChild(subsystem);
            }
        }

        Writer writer = new FileWriter(outputFile);
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlwriter = new FormattingXMLStreamWriter(factory.createXMLStreamWriter(writer));
            config.marshall(xmlwriter);
        } finally {
            writer.close();
        }
    }
}
