/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.extension.undertow.errorhandler.ErrorPageDefinition;
import org.wildfly.extension.undertow.filters.BasicAuthHandler;
import org.wildfly.extension.undertow.filters.ConnectionLimitHandler;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;
import org.wildfly.extension.undertow.handlers.FileHandler;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class UndertowSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {
    protected static final UndertowSubsystemParser_1_0 INSTANCE = new UndertowSubsystemParser_1_0();


    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(UndertowRootDefinition.INSTANCE)
                .addAttributes(UndertowRootDefinition.DEFAULT_VIRTUAL_HOST, UndertowRootDefinition.DEFAULT_SERVLET_CONTAINER, UndertowRootDefinition.DEFAULT_SERVER, UndertowRootDefinition.INSTANCE_ID)
                .addChild(

                        builder(BufferCacheDefinition.INSTANCE)
                                .addAttributes(BufferCacheDefinition.BUFFER_SIZE, BufferCacheDefinition.BUFFERS_PER_REGION, BufferCacheDefinition.MAX_REGIONS)
                                .setXmlWrapperElement(Constants.BUFFER_CACHES)
                )
                .addChild(builder(ServerDefinition.INSTANCE)
                        .addAttributes(ServerDefinition.DEFAULT_HOST, ServerDefinition.SERVLET_CONTAINER)
                        .addChild(
                                builder(AjpListenerResourceDefinition.INSTANCE)
                                        .addAttributes(AjpListenerResourceDefinition.SCHEME)
                                        .addAttributes(AjpListenerResourceDefinition.ATTRIBUTES)
                        )
                        .addChild(
                                builder(HttpListenerResourceDefinition.INSTANCE)
                                        .addAttributes(HttpListenerResourceDefinition.ATTRIBUTES)
                        ).addChild(
                                builder(HttpsListenerResourceDefinition.INSTANCE)
                                        .addAttributes(HttpsListenerResourceDefinition.INSTANCE.getAttributes())
                        ).addChild(
                                builder(HostDefinition.INSTANCE)
                                        .addAttributes(HostDefinition.ALIAS)
                                        .addChild(
                                                builder(LocationDefinition.INSTANCE)
                                                        .addAttributes(LocationDefinition.HANDLER)
                                                        .addChild(
                                                                builder(FilterRefDefinition.INSTANCE)
                                                        )
                                        )
                        )
                )
                .addChild(
                        builder(ServletContainerDefinition.INSTANCE)
                                .addAttribute(ServletContainerDefinition.DEVELOPMENT_MODE)
                                .addChild(
                                        builder(JspDefinition.INSTANCE)
                                                .setXmlElementName(Constants.JSP_CONFIG)
                                                .addAttributes(
                                                        JspDefinition.DISABLED,
                                                        JspDefinition.KEEP_GENERATED,
                                                        JspDefinition.TRIM_SPACES,
                                                        JspDefinition.TAG_POOLING,
                                                        JspDefinition.MAPPED_FILE,
                                                        JspDefinition.CHECK_INTERVAL,
                                                        JspDefinition.MODIFICATION_TEST_INTERVAL,
                                                        JspDefinition.RECOMPILE_ON_FAIL,
                                                        JspDefinition.SMAP,
                                                        JspDefinition.DUMP_SMAP,
                                                        JspDefinition.GENERATE_STRINGS_AS_CHAR_ARRAYS,
                                                        JspDefinition.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE,
                                                        JspDefinition.SCRATCH_DIR,
                                                        JspDefinition.SOURCE_VM,
                                                        JspDefinition.TARGET_VM,
                                                        JspDefinition.JAVA_ENCODING,
                                                        JspDefinition.X_POWERED_BY,
                                                        JspDefinition.DISPLAY_SOURCE_FRAGMENT)
                                )
                                .addChild(
                                        builder(SessionCookieDefinition.INSTANCE)
                                                .addAttributes(
                                                        SessionCookieDefinition.NAME,
                                                        SessionCookieDefinition.DOMAIN,
                                                        SessionCookieDefinition.COMMENT,
                                                        SessionCookieDefinition.HTTP_ONLY,
                                                        SessionCookieDefinition.SECURE,
                                                        SessionCookieDefinition.MAX_AGE
                                                )
                                )
                )
                .addChild(
                        builder(ErrorPageDefinition.INSTANCE)
                                                .addAttributes(ErrorPageDefinition.CODE, ErrorPageDefinition.PATH)
                                                .setXmlWrapperElement(Constants.ERROR_PAGES))
                .addChild(
                        builder(HandlerDefinitions.INSTANCE)
                                .setXmlElementName(Constants.HANDLERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(FileHandler.INSTANCE)
                                                .addAttributes(
                                                        FileHandler.PATH,
                                                        FileHandler.CACHE_BUFFER_SIZE,
                                                        FileHandler.CACHE_BUFFERS,
                                                        FileHandler.DIRECTORY_LISTING)
                                )


                )
                .addChild(
                        builder(FilterDefinitions.INSTANCE)
                                .setXmlElementName(Constants.FILTERS)
                                .setNoAddOperation(true)
                                .addChild(
                                        builder(BasicAuthHandler.INSTANCE)
                                                .addAttributes(BasicAuthHandler.SECURITY_DOMAIN)
                                )
                                .addChild(
                                        builder(ConnectionLimitHandler.INSTANCE)
                                        .addAttributes(ConnectionLimitHandler.MAX_CONCURRENT_REQUESTS)
                                )

                )
                .setAdditionalOperationsGenerator(new PersistentResourceXMLDescription.AdditionalOperationsGenerator() {
                    @Override
                    public void additionalOperations(final PathAddress address, final ModelNode addOperation, final List<ModelNode> operations) {
                        operations.add(Util.createAddOperation(address.append(UndertowExtension.PATH_FILTERS)));
                        operations.add(Util.createAddOperation(address.append(UndertowExtension.PATH_HANDLERS)));
                    }
                })
                .build();
    }

    private UndertowSubsystemParser_1_0() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        ModelNode model = new ModelNode();
        model.get(UndertowRootDefinition.INSTANCE.getPathElement().getKeyValuePair()).set(context.getModelNode());//this is bit of workaround for SPRD to work properly
        xmlDescription.persist(writer, model, Namespace.CURRENT.getUriString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        xmlDescription.parse(reader, PathAddress.EMPTY_ADDRESS, list);
    }
}

