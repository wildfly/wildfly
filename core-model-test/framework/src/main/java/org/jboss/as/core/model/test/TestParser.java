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
package org.jboss.as.core.model.test;

import java.util.List;
import java.util.concurrent.Executors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.host.controller.parsing.DomainXml;
import org.jboss.as.host.controller.parsing.HostXml;
import org.jboss.as.model.test.ModelTestParser;
import org.jboss.as.server.parsing.StandaloneXml;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestParser implements ModelTestParser {
    private final TestModelType type;
    private final XMLElementReader<List<ModelNode>> reader;
    private final XMLElementWriter<ModelMarshallingContext> writer;
    private volatile ModelWriteSanitizer writeSanitizer;

    public TestParser(TestModelType type, XMLElementReader<List<ModelNode>> reader, XMLElementWriter<ModelMarshallingContext> writer) {
        this.type = type;
        this.reader = reader;
        this.writer = writer;
    }

    public static TestParser create(XMLMapper xmlMapper, TestModelType type) {
        TestParser testParser;
        String root;
        if (type == TestModelType.STANDALONE) {
            StandaloneXml standaloneXml = new StandaloneXml(null, Executors.newCachedThreadPool(), null);
            testParser = new TestParser(type, standaloneXml, standaloneXml);
            root = "server";
        } else if (type == TestModelType.DOMAIN) {
            DomainXml domainXml = new DomainXml(null, Executors.newCachedThreadPool(), null);
            testParser = new TestParser(type, domainXml, domainXml);
            root = "domain";
        } else if (type == TestModelType.HOST) {
            HostXml hostXml = new HostXml("master");
            testParser = new TestParser(type, hostXml, hostXml);
            root = "host";
        } else {
            throw new IllegalArgumentException("Unknown type " + type);
        }


        try {
            for (Namespace ns : Namespace.ALL_NAMESPACES) {
                xmlMapper.registerRootElement(new QName(ns.getUriString(), root), testParser);
            }
        } catch (NoSuchFieldError e) {
            //7.1.2 does not have the ALL_NAMESPACES field
            xmlMapper.registerRootElement(new QName(Namespace.DOMAIN_1_0.getUriString(), root), testParser);
            xmlMapper.registerRootElement(new QName(Namespace.DOMAIN_1_1.getUriString(), root), testParser);
            xmlMapper.registerRootElement(new QName(Namespace.DOMAIN_1_2.getUriString(), root), testParser);
        }
        return testParser;
    }

    void setModelWriteSanitizer(ModelWriteSanitizer writeSanitizer) {
        this.writeSanitizer = writeSanitizer;
    }

    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> value) throws XMLStreamException {
        this.reader.readElement(reader, value);
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter, ModelMarshallingContext context) throws XMLStreamException {
        this.writer.writeContent(streamWriter, sanitizeContext(wrapPossibleHost(context)));
    }

    private ModelMarshallingContext wrapPossibleHost(final ModelMarshallingContext context) {

        if (type == TestModelType.HOST) {
            return new ModelMarshallingContext() {

                @Override
                public XMLElementWriter<SubsystemMarshallingContext> getSubsystemWriter(String subsystemName) {
                    return context.getSubsystemWriter(subsystemName);
                }

                @Override
                public XMLElementWriter<SubsystemMarshallingContext> getSubsystemDeploymentWriter(String subsystemName) {
                    return context.getSubsystemDeploymentWriter(subsystemName);
                }

                @Override
                public ModelNode getModelNode() {
                    return context.getModelNode().get(ModelDescriptionConstants.HOST, "master");
                }
            };
        }

        return context;
    }
    private ModelMarshallingContext sanitizeContext(final ModelMarshallingContext context) {
        if (writeSanitizer == null) {
            return context;
        }
        final ModelNode model = writeSanitizer.sanitize(context.getModelNode());
        return new ModelMarshallingContext() {

            @Override
            public XMLElementWriter<SubsystemMarshallingContext> getSubsystemWriter(String subsystemName) {
                return context.getSubsystemWriter(subsystemName);
            }

            @Override
            public XMLElementWriter<SubsystemMarshallingContext> getSubsystemDeploymentWriter(String subsystemName) {
                return context.getSubsystemDeploymentWriter(subsystemName);
            }

            @Override
            public ModelNode getModelNode() {
                return model;
            }
        };
    }
}
