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

package org.jboss.as.controller.parsing;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.parsing.ParseUtils.invalidAttributeValue;
import static org.jboss.as.controller.parsing.ParseUtils.readStringAttributeElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNamespace;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * Parsing and marshalling logic related to the {@code extension} element in standalone.xml and domain.xml.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionXml {

    private final ModuleLoader moduleLoader;
    private final ExecutorService bootExecutor;
    private final ExtensionRegistry extensionRegistry;

    public ExtensionXml(final ModuleLoader loader, final ExecutorService executorService, final ExtensionRegistry extensionRegistry) {
        moduleLoader = loader;
        bootExecutor = executorService;
        this.extensionRegistry = extensionRegistry;
    }

    public void writeExtensions(final XMLExtendedStreamWriter writer, final ModelNode modelNode) throws XMLStreamException {
        Set<String> keys = modelNode.keys();
        if (keys.size() > 0) {
            writer.writeStartElement(Element.EXTENSIONS.getLocalName());
            for (final String extension : keys) {
                writer.writeEmptyElement(Element.EXTENSION.getLocalName());
                writer.writeAttribute(Attribute.MODULE.getLocalName(), extension);
            }
            writer.writeEndElement();
        }
    }

    public void parseExtensions(final XMLExtendedStreamReader reader, final ModelNode address, final Namespace expectedNs, final List<ModelNode> list)
            throws XMLStreamException {

        long start = System.currentTimeMillis();

        requireNoAttributes(reader);

        final Set<String> found = new HashSet<String>();

        final XMLMapper xmlMapper = reader.getXMLMapper();

        final Map<String, Future<XMLStreamException>> loadFutures = bootExecutor != null
                ? new LinkedHashMap<String, Future<XMLStreamException>>() : null;

        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            requireNamespace(reader, expectedNs);
            final Element element = Element.forName(reader.getLocalName());
            if (element != Element.EXTENSION) {
                throw unexpectedElement(reader);
            }

            // One attribute && require no content
            final String moduleName = readStringAttributeElement(reader, Attribute.MODULE.getLocalName());

            if (!found.add(moduleName)) {
                // duplicate module name
                throw invalidAttributeValue(reader, 0);
            }

            if (loadFutures != null) {
                // Load the module asynchronously
                Callable<XMLStreamException> callable = new Callable<XMLStreamException>() {
                    @Override
                    public XMLStreamException call() throws Exception {
                        return loadModule(moduleName, xmlMapper);
                    }
                };
                Future<XMLStreamException> future = bootExecutor.submit(callable);
                loadFutures.put(moduleName, future);
            } else {
                // Load the module from this thread
                XMLStreamException xse = loadModule(moduleName, xmlMapper);
                if (xse != null) {
                    throw xse;
                }
                addExtensionAddOperation(address, list, moduleName);
            }

        }

        if (loadFutures != null) {
            for (Map.Entry<String, Future<XMLStreamException>> entry : loadFutures.entrySet()) {

                try {
                    XMLStreamException xse = entry.getValue().get();
                    if (xse != null) {
                        throw xse;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw MESSAGES.moduleLoadingInterrupted(entry.getKey());
                } catch (ExecutionException e) {
                    throw MESSAGES.failedToLoadModule(e, entry.getKey());
                }

                addExtensionAddOperation(address, list, entry.getKey());
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        if (ROOT_LOGGER.isDebugEnabled()) {
            ROOT_LOGGER.debugf("Parsed extensions in [%d] ms", elapsed);
        }
    }

    private void addExtensionAddOperation(ModelNode address, List<ModelNode> list, String moduleName) {
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(address).add(EXTENSION, moduleName);
        add.get(OP).set(ADD);
        list.add(add);
    }

    private XMLStreamException loadModule(final String moduleName, final XMLMapper xmlMapper) throws XMLStreamException {
        // Register element handlers for this extension
        try {
            final Module module = moduleLoader.loadModule(ModuleIdentifier.fromString(moduleName));
            boolean initialized = false;
            for (final Extension extension : module.loadService(Extension.class)) {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(extension.getClass());
                try {
                    extension.initializeParsers(extensionRegistry.getExtensionParsingContext(moduleName, xmlMapper));
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }
                if (!initialized) {
                    initialized = true;
                }
            }
            if (!initialized) {
                throw MESSAGES.notFound("META-INF/services/", Extension.class.getName(), module.getIdentifier());
            }
            return null;
        } catch (final ModuleLoadException e) {
            throw MESSAGES.failedToLoadModule(e, moduleName);
        }
    }
}
