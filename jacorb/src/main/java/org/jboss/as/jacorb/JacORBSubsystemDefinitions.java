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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.as.jacorb;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * <p>
 * This class contains all JacORB subsystem attribute definitions.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class JacORBSubsystemDefinitions {

    private static final ModelNode DEFAULT_DISABLED_PROPERTY = new ModelNode().set("off");

    private static final ModelNode DEFAULT_ENABLED_PROPERTY = new ModelNode().set("on");

    // orb attribute definitions.
    public static final SimpleAttributeDefinition ORB_NAME = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.NAME, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("JBoss"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_PRINT_VERSION = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_PRINT_VERSION, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_USE_IMR = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_USE_IMR, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_USE_BOM = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_USE_BOM, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CACHE_TYPECODES = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CACHE_TYPECODES, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CACHE_POA_NAMES = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CACHE_POA_NAMES, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_GIOP_MINOR_VERSION = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(2))
            .setValidator(new IntRangeValidator(1, 2, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // connection attribute definitions.
    public static final SimpleAttributeDefinition ORB_CONN_RETRIES = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_RETRIES, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(5))
            .setValidator(new IntRangeValidator(0, 50, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_RETRY_INTERVAL = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_RETRY_INTERVAL, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(500))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_CLIENT_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_CLIENT_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(0))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_SERVER_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_SERVER_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(0))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_MAX_SERVER_CONNECTIONS = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_MAX_SERVER_CONNECTIONS, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(Integer.MAX_VALUE))
            .setValidator(new IntRangeValidator(0, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_MAX_MANAGED_BUF_SIZE = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_MAX_MANAGED_BUF_SIZE, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(24))
            .setValidator(new IntRangeValidator(0, 64, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_OUTBUF_SIZE = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_OUTBUF_SIZE, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(2048))
            .setValidator(new IntRangeValidator(0, 65536, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_CONN_OUTBUF_CACHE_TIMEOUT = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_CONN_OUTBUF_CACHE_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(-1))
            .setValidator(new IntRangeValidator(-1, Integer.MAX_VALUE, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // initializers attribute definitions.
    public static final SimpleAttributeDefinition ORB_INIT_SECURITY = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_INIT_SECURITY, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition ORB_INIT_TX = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // poa attribute definitions.
    public static final SimpleAttributeDefinition POA_MONITORING = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.POA_MONITORING, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_QUEUE_WAIT = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.POA_QUEUE_WAIT, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_QUEUE_MIN = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.POA_QUEUE_MIN, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(10))
            .setValidator(new IntRangeValidator(1, 100, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_QUEUE_MAX = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.POA_QUEUE_MAX, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(100))
            .setValidator(new IntRangeValidator(1, 200, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // request processor attribute definitions.
    public static final SimpleAttributeDefinition POA_REQUEST_PROC_POOL_SIZE = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.POA_RP_POOL_SIZE, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(5))
            .setValidator(new IntRangeValidator(1, 100, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition POA_REQUEST_PROC_MAX_THREADS = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.POA_RP_MAX_THREADS, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(32))
            .setValidator(new IntRangeValidator(5, 150, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // naming attribute definitions.
    public static final SimpleAttributeDefinition NAMING_ROOT_CONTEXT = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.NAMING_ROOT_CONTEXT, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("JBoss/Naming/root"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition NAMING_EXPORT_CORBALOC = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // interoperability attribute definitions.
    public static final SimpleAttributeDefinition INTEROP_SUN = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_SUN, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_COMET = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_COMET, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_IONA = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_IONA, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_CHUNK_RMI_VALUETYPES = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_CHUNK_RMI_VALUETYPES, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_LAX_BOOLEAN_ENCODING = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_LAX_BOOLEAN_ENCODING, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_INDIRECT_ENCODING_DISABLE = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_INDIRECTION_ENCODING_DISABLE, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition INTEROP_STRICT_CHECK_ON_TC_CREATION = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.INTEROP_STRICT_CHECK_ON_TC_CREATION, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    // security attribute definitions.
    public static final SimpleAttributeDefinition SECURITY_SUPPORT_SSL = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_SUPPORT_SSL, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_SECURITY_DOMAIN, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_ADD_COMPONENT_INTERCEPTOR = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_ENABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_CLIENT_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_CLIENT_SUPPORTS, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(60))
            .setValidator(new IntRangeValidator(0, 60, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_CLIENT_REQUIRES = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_CLIENT_REQUIRES, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(0))
            .setValidator(new IntRangeValidator(0, 60, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SERVER_SUPPORTS = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_SERVER_SUPPORTS, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(60))
            .setValidator(new IntRangeValidator(0, 60, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_SERVER_REQUIRES = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_SERVER_REQUIRES, ModelType.INT, true)
            .setDefaultValue(new ModelNode().set(0))
            .setValidator(new IntRangeValidator(0, 60, true, false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_USE_DOMAIN_SF = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_USE_DOMAIN_SF, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SECURITY_USE_DOMAIN_SSF = new SimpleAttributeDefinitionBuilder(
            JacORBSubsystemConstants.SECURITY_USE_DOMAIN_SSF, ModelType.STRING, true)
            .setDefaultValue(DEFAULT_DISABLED_PROPERTY)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();
    public static final PropertiesAttributeDefinition PROPERTIES =
            new PropertiesAttributeDefinition(JacORBSubsystemConstants.PROPERTIES,
                    JacORBSubsystemConstants.PROPERTIES, true);


    // list that contains the orb attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_ATTRIBUTES = Arrays.asList(ORB_NAME, ORB_PRINT_VERSION,
            ORB_USE_IMR, ORB_USE_BOM, ORB_CACHE_TYPECODES, ORB_CACHE_POA_NAMES, ORB_GIOP_MINOR_VERSION);

    // list that contains the orb connection attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_CONN_ATTRIBUTES = Arrays.asList(ORB_CONN_RETRIES,
            ORB_CONN_RETRY_INTERVAL, ORB_CONN_CLIENT_TIMEOUT, ORB_CONN_SERVER_TIMEOUT, ORB_CONN_MAX_SERVER_CONNECTIONS,
            ORB_CONN_MAX_MANAGED_BUF_SIZE, ORB_CONN_OUTBUF_SIZE, ORB_CONN_OUTBUF_CACHE_TIMEOUT);

    // list that contains the orb initializer attribute definitions.
    static final List<SimpleAttributeDefinition> ORB_INIT_ATTRIBUTES = Arrays.asList(ORB_INIT_SECURITY, ORB_INIT_TX);

    // list that contains the poa attribute definitions.
    static final List<SimpleAttributeDefinition> POA_ATTRIBUTES = Arrays.asList(POA_MONITORING, POA_QUEUE_WAIT,
            POA_QUEUE_MIN, POA_QUEUE_MAX);

    // list that contains the poa request processor attribute definitions.
    static final List<SimpleAttributeDefinition> POA_RP_ATTRIBUTES = Arrays.asList(POA_REQUEST_PROC_POOL_SIZE,
            POA_REQUEST_PROC_MAX_THREADS);

    // list that contains the naming attribute definitions.
    static final List<SimpleAttributeDefinition> NAMING_ATTRIBUTES = Arrays.asList(NAMING_ROOT_CONTEXT,
            NAMING_EXPORT_CORBALOC);

    // list that contains the interoperability attribute definitions.
    static final List<SimpleAttributeDefinition> INTEROP_ATTRIBUTES = Arrays.asList(INTEROP_SUN, INTEROP_COMET,
            INTEROP_IONA, INTEROP_CHUNK_RMI_VALUETYPES, INTEROP_LAX_BOOLEAN_ENCODING, INTEROP_INDIRECT_ENCODING_DISABLE,
            INTEROP_STRICT_CHECK_ON_TC_CREATION);

    // list that contains the security attribute definitions.
    static final List<SimpleAttributeDefinition> SECURITY_ATTRIBUTES = Arrays.asList(SECURITY_SUPPORT_SSL,
            SECURITY_SECURITY_DOMAIN, SECURITY_ADD_COMPONENT_INTERCEPTOR, SECURITY_CLIENT_SUPPORTS,
            SECURITY_CLIENT_REQUIRES, SECURITY_SERVER_SUPPORTS, SECURITY_SERVER_REQUIRES, SECURITY_USE_DOMAIN_SF,
            SECURITY_USE_DOMAIN_SSF);

    // list that contains all attribute definitions.
    static final List<AttributeDefinition> SUBSYSTEM_ATTRIBUTES;

    // utility map that keys all definitions by their names.
    static final Map<String, AttributeDefinition> ATTRIBUTES_BY_NAME;

    static {
        SUBSYSTEM_ATTRIBUTES = new ArrayList<AttributeDefinition>();
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_CONN_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(ORB_INIT_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(POA_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(POA_RP_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(NAMING_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(INTEROP_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.addAll(SECURITY_ATTRIBUTES);
        SUBSYSTEM_ATTRIBUTES.add(PROPERTIES);

        Map<String, AttributeDefinition> map = new HashMap<String, AttributeDefinition>();
        for (AttributeDefinition attribute : SUBSYSTEM_ATTRIBUTES) { map.put(attribute.getName(), attribute); }
        ATTRIBUTES_BY_NAME = map;
    }

    /**
     * <p>
     * Gets the {@code SimpleAttributeDefinition} identified by the specified name.
     * </p>
     *
     * @param attributeNAme a {@code String} representing the attribute name.
     * @return the corresponding attribute definition or {@code null} if no definition was found with that name.
     */
    public static AttributeDefinition valueOf(String attributeNAme) {
        return ATTRIBUTES_BY_NAME.get(attributeNAme);
    }


    /**
     * @see org.jboss.as.remoting.AbstractOutboundConnectionResourceDefinition.PropertiesAttributeDefinition
     */
    //todo has to be moved to some better place same as org.jboss.as.remoting.AbstractOutboundConnectionResourceDefinition.PropertiesAttributeDefinition
    private static class PropertiesAttributeDefinition extends MapAttributeDefinition {

        public PropertiesAttributeDefinition(final String name, final String xmlName, boolean allowNull) {
            super(name, xmlName, allowNull, 0, Integer.MAX_VALUE, new ModelTypeValidator(ModelType.STRING));
        }

        @Override
        protected void addValueTypeDescription(ModelNode node, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addAttributeValueTypeDescription(ModelNode node, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        protected void addOperationParameterValueTypeDescription(ModelNode node, String operationName, ResourceDescriptionResolver resolver, Locale locale, ResourceBundle bundle) {
            node.get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
        }

        @Override
        public void marshallAsElement(ModelNode resourceModel, XMLStreamWriter writer) throws XMLStreamException {
            if (!isMarshallable(resourceModel)) { return; }

            resourceModel = resourceModel.get(getName());
            writer.writeStartElement(getName());
            for (ModelNode property : resourceModel.asList()) {
                writer.writeEmptyElement(getXmlName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.asProperty().getName());
                writer.writeAttribute(Attribute.VALUE.getLocalName(), property.asProperty().getValue().asString());
            }
            writer.writeEndElement();
        }
    }
}