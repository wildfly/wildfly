/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.DefaultAttributeMarshaller;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostDefinition extends PersistentResourceDefinition {
    static final StringListAttributeDefinition ALIAS = new StringListAttributeDefinition.Builder(Constants.ALIAS)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setElementValidator(new StringLengthValidator(1))
            .setAttributeParser(new AttributeParser() {
                @Override
                public void parseAndSetParameter(AttributeDefinition attribute, String value, ModelNode operation, XMLStreamReader reader) throws XMLStreamException {
                    if (value == null) { return; }
                    for (String element : value.split(",")) {
                        ModelNode paramVal = parse(attribute, element, reader);
                        operation.get(attribute.getName()).add(paramVal);
                    }
                }
            })
            .setAttributeMarshaller(new DefaultAttributeMarshaller() {
                @Override
                public void marshallAsAttribute(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {

                    StringBuilder builder = new StringBuilder();
                    if (resourceModel.hasDefined(attribute.getName())) {
                        for (ModelNode p : resourceModel.get(attribute.getName()).asList()) {
                            builder.append(p.asString()).append(", ");
                        }
                    }
                    if (builder.length() > 3) {
                        builder.setLength(builder.length() - 2);
                    }
                    if (builder.length() > 0) {
                        writer.writeAttribute(attribute.getXmlName(), builder.toString());
                    }
                }
            })
            .build();
    static final SimpleAttributeDefinition DEFAULT_WEB_MODULE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_WEB_MODULE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setValidator(new StringLengthValidator(1, true, false))
            .setDefaultValue(new ModelNode("ROOT.war"))
            .build();

    static final HostDefinition INSTANCE = new HostDefinition();
    private static final Collection ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(ALIAS, DEFAULT_WEB_MODULE));
    private static final List<? extends PersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(Arrays.asList(
            LocationDefinition.INSTANCE,
            AccessLogDefinition.INSTANCE,
            FilterRefDefinition.INSTANCE,
            SingleSignOnDefinition.INSTANCE

    ));

    private HostDefinition() {
        super(UndertowExtension.HOST_PATH, UndertowExtension.getResolver(Constants.HOST),
                HostAdd.INSTANCE,
                new HostRemove());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return ATTRIBUTES;
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

}
