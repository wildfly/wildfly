/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.appclient.subsystem.parsing;

import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.parsing.ManagementXmlReaderWriter;
import org.jboss.as.controller.parsing.ManagementXmlSchema;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.IntVersion;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class AppClientXml implements ManagementXmlReaderWriter {

    final ModuleLoader loader;
    final ExtensionRegistry extensionRegistry;

    AppClientXml(final ModuleLoader loader, final ExtensionRegistry extensionRegistry) {
        this.loader = loader;
        this.extensionRegistry = extensionRegistry;
    }

    public void readElement(XMLExtendedStreamReader reader, VersionedNamespace<IntVersion, ManagementXmlSchema> namespace, List<ModelNode> value) throws XMLStreamException {
        readElement(reader, namespace.getVersion(), namespace.getUri(), value);
    }

    public void readElement(XMLExtendedStreamReader reader, IntVersion version, String namespaceUri,
            List<ModelNode> value) throws XMLStreamException {
        new AppClientXml_All(loader, extensionRegistry, version, namespaceUri).readElement(reader, value);
    }

    public void writeContent(XMLExtendedStreamWriter streamWriter, VersionedNamespace<IntVersion, ManagementXmlSchema> namespace, ModelMarshallingContext value) throws XMLStreamException {
        writeContent(streamWriter, namespace.getVersion(), namespace.getUri(), value);
    }

    public void writeContent(XMLExtendedStreamWriter streamWriter, IntVersion version, String namespaceUri,
            ModelMarshallingContext value) throws XMLStreamException {
        throw new UnsupportedOperationException("Unimplemented method 'writeContent'");
    }

}
