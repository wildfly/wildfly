/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link AttributeDefinition} implementation for the "static-ejb-discovery" attribute.
 *
 * @author Stuart Douglas
 */
public class StaticEJBDiscoveryDefinition {

    public static final String APP = "app-name";
    public static final String MODULE = "module-name";
    public static final String DISTINCT = "distinct-name";
    public static final String URI = "uri";
    public static final String STATIC_EJB_DISCOVERY = "static-ejb-discovery";

    public static final SimpleAttributeDefinition APP_AD = new SimpleAttributeDefinitionBuilder(APP, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MODULE_AD = new SimpleAttributeDefinitionBuilder(MODULE, ModelType.STRING, false)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition DISTINCT_AD = new SimpleAttributeDefinitionBuilder(DISTINCT, ModelType.STRING, true)
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition URI_AD = new SimpleAttributeDefinitionBuilder(URI, ModelType.STRING, false)
            .setAllowExpression(true)
            .build();


    private static final SimpleAttributeDefinition[] VALUE_TYPE_FIELDS = {URI_AD, APP_AD, MODULE_AD, DISTINCT_AD};

    // TODO the default marshalling in ObjectListAttributeDefinition is not so great since it delegates each
    // element to ObjectTypeAttributeDefinition, and OTAD assumes it's used for complex attributes bound in a
    // ModelType.OBJECT node under key=OTAD.getName(). So provide a custom marshaller to OTAD. This could be made reusable.
    private static final AttributeMarshaller VALUE_TYPE_MARSHALLER = new AttributeMarshaller() {
        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            if (resourceModel.isDefined()) {
                writer.writeEmptyElement(EJB3SubsystemXMLElement.MODULE.getLocalName());
                for (SimpleAttributeDefinition valueType : VALUE_TYPE_FIELDS) {
                    valueType.getAttributeMarshaller().marshall(valueType, resourceModel, true, writer);
                }
            }
        }
    };

    private static final ObjectTypeAttributeDefinition VALUE_TYPE_AD =
            ObjectTypeAttributeDefinition.Builder.of(ModelDescriptionConstants.MODULE, VALUE_TYPE_FIELDS)
                    .setAttributeMarshaller(VALUE_TYPE_MARSHALLER)
                    .build();

    public static final AttributeDefinition INSTANCE = ObjectListAttributeDefinition.Builder.of(STATIC_EJB_DISCOVERY, VALUE_TYPE_AD)
        .setRequired(false)
        .build();

    public static List<StaticEjbDiscovery> createStaticEjbList(final OperationContext context, final ModelNode ejbList) throws OperationFailedException {
        final List<StaticEjbDiscovery> ret = new ArrayList<>();
        if (ejbList.isDefined()) {
            for (final ModelNode disc : ejbList.asList()) {
                ModelNode app = APP_AD.resolveModelAttribute(context, disc);
                String module = MODULE_AD.resolveModelAttribute(context, disc).asString();
                ModelNode distinct = DISTINCT_AD.resolveModelAttribute(context, disc);
                String url = URI_AD.resolveModelAttribute(context, disc).asString();
                ret.add(new StaticEjbDiscovery(app.isDefined() ? app.asString() : null, module, distinct.isDefined() ? distinct.asString() : null, url));
            }
        }
        return ret;
    }

    public static final class StaticEjbDiscovery {
        private final String app;
        private final String module;
        private final String distinct;
        private final String url;

        public StaticEjbDiscovery(String app, String module, String distinct, String url) {
            this.app = app;
            this.module = module;
            this.distinct = distinct;
            this.url = url;
        }

        public String getApp() {
            return app;
        }

        public String getModule() {
            return module;
        }

        public String getDistinct() {
            return distinct;
        }

        public String getUrl() {
            return url;
        }
    }
}
