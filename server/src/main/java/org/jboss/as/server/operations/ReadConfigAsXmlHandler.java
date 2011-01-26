/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.operations;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.Locale;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.parsing.StandaloneXml;
import org.jboss.as.server.controller.descriptions.ServerRootDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLMapper;

/**
 * Handler for the read-config-as-xml operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class ReadConfigAsXmlHandler implements ModelQueryOperationHandler, DescriptionProvider {

    public static final String READ_CONFIG_AS_XML = "read-config-as-xml";

    private static final Logger log = Logger.getLogger("org.jboss.as.server.controller");

    private static final XMLElementWriter<ModelNode> rootDeparser = new StandaloneXml(null);
    private static final String[] EMPTY = new String[0];

    public static final ReadConfigAsXmlHandler INSTANCE = new ReadConfigAsXmlHandler();

    private ReadConfigAsXmlHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ServerRootDescription.getReadConfigAsXmlOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final XMLMapper mapper = XMLMapper.Factory.create();

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                BufferedOutputStream output = new BufferedOutputStream(baos);
                final XMLStreamWriter streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
                mapper.deparseDocument(rootDeparser, context.getSubModel(), streamWriter);
                streamWriter.close();
                output.close();
                baos.close();
            } finally {
                safeClose(baos);
            }
            String xml = new String(baos.toByteArray());
            ModelNode result = new ModelNode().set(xml);
            resultHandler.handleResultFragment(EMPTY, result);
            resultHandler.handleResultComplete(null);
        } catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return null;
    }

    private static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            log.errorf(t, "Failed to close resource %s", closeable);
        }
    }

}
