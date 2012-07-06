/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.jbossallxml;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;
import org.jboss.vfs.VirtualFile;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;

/**
 * DUP that parses jboss-all.xml and attaches the results to the deployment
 *
 * @author Stuart Douglas
 */
public class JBossAllXMLParsingProcessor implements DeploymentUnitProcessor {


    public static final String[] DEPLOYMENT_STRUCTURE_DESCRIPTOR_LOCATIONS = {
            "WEB-INF/jboss-all.xml",
            "META-INF/jboss-all.xml"};

    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();
    public static final String JBOSS = "jboss";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        VirtualFile descriptor = null;
        for (final String loc : DEPLOYMENT_STRUCTURE_DESCRIPTOR_LOCATIONS) {
            final VirtualFile file = root.getRoot().getChild(loc);
            if (file.exists()) {
                descriptor = file;
                break;
            }
        }
        if(descriptor == null) {
            return;
        }
        final XMLMapper mapper = XMLMapper.Factory.create();
        final Map<QName, AttachmentKey<?>> namespaceAttachments = new HashMap<QName, AttachmentKey<?>>();
        for(final JBossAllXMLParserDescription<?> parser : deploymentUnit.getAttachmentList(JBossAllXMLParserDescription.ATTACHMENT_KEY)) {
            namespaceAttachments.put(parser.getRootElement(), parser.getAttachmentKey());
            mapper.registerRootElement(parser.getRootElement(), new JBossAllXMLElementReader(parser));
        }
        mapper.registerRootElement(new QName(Namespace.JBOSS_1_0.getUriString(), JBOSS), Parser.INSTANCE);
        mapper.registerRootElement(new QName(Namespace.NONE.getUriString(), JBOSS), Parser.INSTANCE);

        final JBossAllXmlParseContext context = new JBossAllXmlParseContext(deploymentUnit);
        parse(descriptor, mapper, context);

        //we use this map to detect the presence of two different but functionally equivalent namespaces
        final Map<AttachmentKey<?>, QName> usedNamespaces = new HashMap<AttachmentKey<?>, QName>();
        for(Map.Entry<QName, Object> entry : context.getParseResults().entrySet()) {
            final AttachmentKey attachmentKey = namespaceAttachments.get(entry.getKey());
            if(usedNamespaces.containsKey(attachmentKey)) {
                ServerMessages.MESSAGES.equivilentNamespacesInJBossXml(entry.getKey(), usedNamespaces.get(attachmentKey));
            }
            usedNamespaces.put(attachmentKey, entry.getKey());
            deploymentUnit.putAttachment(attachmentKey, entry.getValue());
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        for(JBossAllXMLParserDescription<?> parser : context.getAttachmentList(JBossAllXMLParserDescription.ATTACHMENT_KEY)) {
            context.removeAttachment(parser.getAttachmentKey());
        }
    }

    private void parse(final VirtualFile file,final XMLMapper mapper, final JBossAllXmlParseContext context) throws DeploymentUnitProcessingException {
        final FileInputStream fis;
        final File realFile;
        try {
            realFile = file.getPhysicalFile();
            fis = new FileInputStream(realFile);
        } catch (IOException e) {
            //should never happen as we check for existence
            throw new DeploymentUnitProcessingException(e);
        }
        try {
            parse(fis, realFile, mapper, context);
        } finally {
            safeClose(fis);
        }
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    private void parse(final InputStream source, final File file,final XMLMapper mapper, final JBossAllXmlParseContext context) throws DeploymentUnitProcessingException {
        try {

            final XMLInputFactory inputFactory = INPUT_FACTORY;
            setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
            setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
            try {
                mapper.parseDocument(context, streamReader);
            } finally {
                safeClose(streamReader);
            }
        } catch (XMLStreamException e) {
            throw ServerMessages.MESSAGES.errorLoadingJBossXmlFile(file.getPath(), e);
        }
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                // ignore
            }
    }

    private static void safeClose(final XMLStreamReader streamReader) {
        if (streamReader != null)
            try {
                streamReader.close();
            } catch (XMLStreamException e) {
                // ignore
            }
    }


    private static class Parser implements XMLElementReader<JBossAllXmlParseContext> {

        public static final Parser INSTANCE = new Parser();

        @Override
        public void readElement(final XMLExtendedStreamReader reader, final JBossAllXmlParseContext context)
                throws XMLStreamException {

            if (Element.forName(reader.getLocalName()) != Element.JBOSS) {
                throw unexpectedElement(reader);
            }

            Namespace readerNS = Namespace.forUri(reader.getNamespaceURI());
            switch (readerNS) {
                case NONE:
                case JBOSS_1_0: {
                    parseJBossElement(reader, context);
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        private void parseJBossElement(final XMLExtendedStreamReader reader, final JBossAllXmlParseContext context) throws XMLStreamException {
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                reader.handleAny(context);
            }
        }
    }

    private static enum Element {

        JBOSS(JBossAllXMLParsingProcessor.JBOSS),
        UNKNOWN("unknown"),
        ;

        private final String name;

        Element(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }
    }

    private static enum Namespace {
        // must be first
        UNKNOWN(null), NONE(null),

        // predefined standard
        XML_SCHEMA_INSTANCE("http://www.w3.org/2001/XMLSchema-instance"),

        // domain versions, oldest to newest
        JBOSS_1_0("urn:jboss:1.0"),
        ;

        /**
         * The current namespace version.
         */
        public static final Namespace CURRENT = JBOSS_1_0;

        private final String name;

        Namespace(final String name) {
            this.name = name;
        }

        /**
         * Get the URI of this namespace.
         *
         * @return the URI
         */
        public String getUriString() {
            return name;
        }

        private static final Map<String, Namespace> MAP;

        static {
            final Map<String, Namespace> map = new HashMap<String, Namespace>();
            for (Namespace namespace : values()) {
                final String name = namespace.getUriString();
                if (name != null)
                    map.put(name, namespace);
            }
            MAP = map;
        }

        public static Namespace forUri(String uri) {
            // FIXME when STXM-8 is done, remove the null check
            if (uri == null || XMLConstants.NULL_NS_URI.equals(uri))
                return NONE;
            final Namespace element = MAP.get(uri);
            return element == null ? UNKNOWN : element;
        }

    }
}
