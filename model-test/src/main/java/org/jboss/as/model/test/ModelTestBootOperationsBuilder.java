/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.model.test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.dmr.ModelNode;

/**
 * Internal class.
 * Used to create the boot operations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ModelTestBootOperationsBuilder {
    private final Class<?> testClass;
    private final BootOperationParser xmlParser;
    private List<ModelNode> bootOperations = Collections.emptyList();
    private String subsystemXml;
    private String subsystemXmlResource;
    private boolean built;

    public ModelTestBootOperationsBuilder(Class<?> testClass, BootOperationParser xmlParser) {
        this.testClass = testClass;
        this.xmlParser = xmlParser;
    }

    public ModelTestBootOperationsBuilder setXmlResource(String resource) throws IOException, XMLStreamException {
        validateNotAlreadyBuilt();
        validateSubsystemConfig();
        this.subsystemXmlResource = resource;
        internalSetSubsystemXml(ModelTestUtils.readResource(testClass, resource));
        return this;
    }

    public ModelTestBootOperationsBuilder setXml(String subsystemXml) throws XMLStreamException {
        validateNotAlreadyBuilt();
        validateSubsystemConfig();
        this.subsystemXml = subsystemXml;
        bootOperations = xmlParser.parse(subsystemXml);
        return this;
    }

    public ModelTestBootOperationsBuilder setBootOperations(List<ModelNode> bootOperations) {
        validateNotAlreadyBuilt();
        validateSubsystemConfig();
        this.bootOperations = bootOperations;
        return this;
    }

    private void internalSetSubsystemXml(String subsystemXml) throws XMLStreamException {
        this.subsystemXml = subsystemXml;
        bootOperations = xmlParser.parse(subsystemXml);
    }

    private void validateSubsystemConfig() {
        if (subsystemXmlResource != null) {
            throw new IllegalArgumentException("Xml resource is already set");
        }
        if (subsystemXml != null) {
            throw new IllegalArgumentException("Xml string is already set");
        }
        if (bootOperations != Collections.EMPTY_LIST) {
            throw new IllegalArgumentException("Boot operations are already set");
        }
    }

    public void validateNotAlreadyBuilt() {
        if (built) {
            throw new IllegalStateException("Already built");
        }
    }

    public List<ModelNode> build() {
        built = true;
        return bootOperations;
    }

    public interface BootOperationParser {
        List<ModelNode> parse(String subsystemXml) throws XMLStreamException;
    }
}
