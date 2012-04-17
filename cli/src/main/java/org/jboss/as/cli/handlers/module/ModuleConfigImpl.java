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
package org.jboss.as.cli.handlers.module;

import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author Alexey Loubyansky
 */
public class ModuleConfigImpl implements ModuleConfig {

    static final String DEPENDENCIES = "dependencies";
    static final String MAIN_CLASS = "main-class";
    static final String MODULE = "module";
    static final String MODULE_NS = "urn:jboss:module:1.1";
    static final String NAME = "name";
    static final String PATH = "path";
    static final String PROPERTY = "property";
    static final String PROPERTIES = "properties";
    static final String RESOURCES = "resources";
    static final String RESOURCE_ROOT = "resource-root";
    static final String VALUE = "value";


    private String schemaVersion = MODULE_NS;

    private String moduleName;
    private String mainClass;

    private Collection<Resource> resources;
    private Collection<Dependency> dependencies;
    private Map<String, String> properties;

    public ModuleConfigImpl(String moduleName) {
        if(moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("Module name can't be null");
        }

        this.moduleName = moduleName;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.module.ModuleConfig#getSchemaVersion()
     */
    @Override
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.module.ModuleConfig#getModuleName()
     */
    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.module.ModuleConfig#getResources()
     */
    @Override
    public Collection<Resource> getResources() {
        return resources == null ? Collections.<Resource>emptyList() : resources;
    }

    public void addResource(Resource res) {
        if(res == null) {
            throw new IllegalArgumentException("Resource cannot be null.");
        }
        if(resources == null) {
            resources = new ArrayList<Resource>();
        }
        resources.add(res);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.module.ModuleConfig#getDependencies()
     */
    @Override
    public Collection<Dependency> getDependencies() {
        return dependencies == null ? Collections.<Dependency>emptyList() : dependencies;
    }

    public void addDependency(Dependency dep) {
        if(dep == null) {
            throw new IllegalArgumentException("Dependency cannot be null.");
        }
        if(dependencies == null) {
            dependencies = new ArrayList<Dependency>();
        }
        dependencies.add(dep);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.handlers.module.ModuleConfig#getProperties()
     */
    @Override
    public Map<String, String> getProperties() {
        return properties == null ? Collections.<String, String>emptyMap() : properties;
    }

    public void setProperty(String name, String value) {
        if(name == null) {
            throw new IllegalArgumentException("Property name can't be null.");
        }
        if(value == null) {
            throw new IllegalArgumentException("Property value can't be null.");
        }
        if(properties == null) {
            properties = new HashMap<String, String>();
        }
        properties.put(name, value);
    }

    /* (non-Javadoc)
     * @see org.jboss.staxmapper.XMLElementWriter#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter, java.lang.Object)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, ModuleConfig value) throws XMLStreamException {

        writer.writeStartDocument();
        writer.writeStartElement(MODULE);
        writer.writeDefaultNamespace(MODULE_NS);

        if(moduleName == null) {
            throw new XMLStreamException("Module name is missing.");
        }
        writer.writeAttribute(NAME, moduleName);

        if(properties != null) {
            writeNewLine(writer);
            writer.writeStartElement(PROPERTIES);
            for(Map.Entry<String, String> entry: properties.entrySet()) {
                writer.writeStartElement(PROPERTY);
                writer.writeAttribute(NAME, entry.getKey());
                writer.writeAttribute(VALUE, entry.getValue());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        if(mainClass != null) {
            writeNewLine(writer);
            writer.writeStartElement(MAIN_CLASS);
            writer.writeAttribute(VALUE, mainClass);
            writer.writeEndElement();
        }

        if(resources != null) {
            writeNewLine(writer);
            writer.writeStartElement(RESOURCES);
            for(Resource res : resources) {
                res.writeContent(writer, res);
            }
            writer.writeEndElement();
        }

        if(dependencies != null) {
            writeNewLine(writer);
            writer.writeStartElement(DEPENDENCIES);
            for(Dependency dep : dependencies) {
                dep.writeContent(writer, dep);
            }
            writer.writeEndElement();
        }

        writeNewLine(writer);
        writer.writeEndElement();
        writer.writeEndDocument();
    }

    // copied from CommonXml

    private static final char[] NEW_LINE = new char[]{'\n'};

    protected static void writeNewLine(XMLExtendedStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters(NEW_LINE, 0, 1);
    }

    // test

    public static void main(String[] args) throws Exception {

        ModuleConfigImpl module = new ModuleConfigImpl("o.r.g");

        module.setProperty("jboss.api", "private");
        module.setProperty("prop", VALUE);

        module.setMainClass("o.r.g.MainClass");

        module.addResource(new ResourceRoot("a.jar"));
        module.addResource(new ResourceRoot("another.jar"));

        module.addDependency(new ModuleDependency("a.module"));
        module.addDependency(new ModuleDependency("another.module"));

        StringWriter strWriter = new StringWriter();
        XMLExtendedStreamWriter writer = create(XMLOutputFactory.newInstance().createXMLStreamWriter(strWriter));
        module.writeContent(writer, module);
        writer.flush();
        System.out.println(strWriter.toString());
    }

    public static XMLExtendedStreamWriter create(XMLStreamWriter writer) throws Exception {
        // Use reflection to access package protected class FormattingXMLStreamWriter
        // TODO: at some point the staxmapper API could be enhanced to make this unnecessary
        Class<?> clazz = Class.forName("org.jboss.staxmapper.FormattingXMLStreamWriter");
        Object [] args = new Object [1];
        args[0] = writer;
        Constructor<?> ctr = clazz.getConstructor( XMLStreamWriter.class );
        ctr.setAccessible(true);
        return (XMLExtendedStreamWriter)ctr.newInstance(args);
    }
}
