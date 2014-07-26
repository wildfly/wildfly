/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.jmx.CommonAttributes.JMX;
import static org.jboss.as.jmx.CommonAttributes.REMOTING_CONNECTOR;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.JmxAuthorizer;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry.ExtensionContextImpl;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.jmx.logging.JmxLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * Domain extension used to initialize the JMX subsystem.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class JMXExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jmx";
    private static final String RESOURCE_NAME = JMXExtension.class.getPackage().getName() + ".LocalDescriptions";


    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, JMXExtension.class.getClassLoader(), true, false);
    }

    static final SensitivityClassification JMX_SENSITIVITY =
            new SensitivityClassification(SUBSYSTEM_NAME, "jmx", false, false, true);

    static final SensitiveTargetAccessConstraintDefinition JMX_SENSITIVITY_DEF = new SensitiveTargetAccessConstraintDefinition(JMX_SENSITIVITY);
    static final JMXSubsystemParser_1_3 parserCurrent = new JMXSubsystemParser_1_3();
    static final JMXSubsystemParser_1_2 parser12 = new JMXSubsystemParser_1_2();
    static final JMXSubsystemParser_1_1 parser11 = new JMXSubsystemParser_1_1();
    static final JMXSubsystemParser_1_0 parser10 = new JMXSubsystemParser_1_0();
    static final JMXSubsystemWriter writer = new JMXSubsystemWriter();

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        //This is ugly but for now we don't want to make the audit logger easily available to all extensions
        ManagedAuditLogger auditLogger = (ManagedAuditLogger)((ExtensionContextImpl)context).getAuditLogger(false, true);
        //This is ugly but for now we don't want to make the authorizer easily available to all extensions
        JmxAuthorizer authorizer = ((ExtensionContextImpl)context).getAuthorizer();

        registration.registerSubsystemModel(JMXSubsystemRootResource.create(auditLogger, authorizer));
        registration.registerXMLElementWriter(writer);

        if (context.isRegisterTransformers()) {
            registerTransformers(registration);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_0.getUriString(), parser10);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_1.getUriString(), parser11);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_2.getUriString(), parser12);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JMX_1_3.getUriString(), parserCurrent);
    }

    private static ModelNode createAddOperation() {
        return createOperation(ADD);
    }

    private static ModelNode createOperation(String name, String... addressElements) {
        final ModelNode op = new ModelNode();
        op.get(OP).set(name);
        op.get(OP_ADDR).add(SUBSYSTEM, SUBSYSTEM_NAME);
        for (int i = 0; i < addressElements.length; i++) {
            op.get(OP_ADDR).add(addressElements[i], addressElements[++i]);
        }
        return op;
    }

    private void registerTransformers(SubsystemRegistration registration) {
        final ModelVersion version1_1_0 = ModelVersion.create(1, 1, 0);
        final ModelVersion version1_0_0 = ModelVersion.create(1, 0, 0);

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getSubsystemVersion());

        //Current 1.2.0 to 1.1.0
        buildTransformers1_1_0(chainedBuilder.createBuilder(registration.getSubsystemVersion(), version1_1_0));

        //1.1.0 to 1.0.0
        buildTransformers1_0_0(chainedBuilder.createBuilder(version1_1_0, version1_0_0));

        chainedBuilder.buildAndRegister(registration, new ModelVersion[]{version1_0_0, version1_1_0});
    }

    private void buildTransformers1_0_0(ResourceTransformationDescriptionBuilder builder) {
        builder.setCustomResourceTransformer(new ResourceTransformer() {
            @Override
            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
                    throws OperationFailedException {
                ModelNode model = resource.getModel();

                // The existence of the expose-model=>resolved child is
                // translated into the show-model=>true attribute
                Resource exposeResolvedResource = resource.getChild(PathElement.pathElement(CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED));
                boolean showModel = false;
                if (exposeResolvedResource != null) {
                    showModel = model.isDefined();
                }
                model.get(CommonAttributes.SHOW_MODEL).set(showModel);
                ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);

                // Process all the child resources skipping the expose-model=>*
                // children
                for (String type : resource.getChildTypes()) {
                    if (!type.equals(CommonAttributes.EXPOSE_MODEL)) {
                        for (ResourceEntry child : resource.getChildren(type)) {
                            childContext.processChild(child.getPathElement(), child);
                        }
                    }
                }
            }
        });
        builder.rejectChildResource(PathElement.pathElement(CommonAttributes.EXPOSE_MODEL, CommonAttributes.EXPRESSION));
        ResourceTransformationDescriptionBuilder resolvedBuilder = builder.addChildResource(PathElement.pathElement(CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED));
        resolvedBuilder.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, CommonAttributes.DOMAIN_NAME, CommonAttributes.PROPER_PROPERTY_FORMAT)
            .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                @Override
                public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                    return JmxLogger.ROOT_LOGGER.domainNameMustBeJBossAs();
                }

                @Override
                protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                        TransformationContext context) {
                    return attributeValue.isDefined() && !attributeValue.asString().equals("jboss.as");
                }
            }, CommonAttributes.DOMAIN_NAME)
            .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                @Override
                public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                    return JmxLogger.ROOT_LOGGER.properPropertyFormatMustBeFalse();
                }

                @Override
                public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue,
                        ModelNode operation, TransformationContext context) {
                    return super.rejectOperationParameter(address, attributeName, attributeValue, operation, context);
                }

                @Override
                protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                        TransformationContext context) {
                    return !attributeValue.isDefined() || (attributeValue.getType() == ModelType.BOOLEAN && attributeValue.asBoolean());
                }
            }, CommonAttributes.PROPER_PROPERTY_FORMAT);
        resolvedBuilder.setCustomResourceTransformer(ResourceTransformer.DISCARD);
        resolvedBuilder.addOperationTransformationOverride(ADD).inheritResourceAttributeDefinitions().setCustomOperationTransformer(new ResolvedOperationTransformer(true));
        resolvedBuilder.addOperationTransformationOverride(REMOVE).inheritResourceAttributeDefinitions().setCustomOperationTransformer(new ResolvedOperationTransformer(false));
        //No need to do write-attribute and undefine-attribute, null becomes true and any other value than 'jboss.as' is rejected
        resolvedBuilder.addOperationTransformationOverride(UNDEFINE_ATTRIBUTE_OPERATION).inheritResourceAttributeDefinitions().setCustomOperationTransformer(OperationTransformer.DISCARD);
        resolvedBuilder.addOperationTransformationOverride(WRITE_ATTRIBUTE_OPERATION).inheritResourceAttributeDefinitions().setCustomOperationTransformer(OperationTransformer.DISCARD);
        resolvedBuilder.addOperationTransformationOverride(READ_ATTRIBUTE_OPERATION).setCustomOperationTransformer(new OperationTransformer() {
            @Override
            public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
                    throws OperationFailedException {
                return new TransformedOperation(null, new OperationResultTransformer() {

                    @Override
                    public ModelNode transformResult(ModelNode result) {
                        if (operation.get(NAME).asString().equals(CommonAttributes.DOMAIN_NAME)) {
                            result.get(RESULT).set(CommonAttributes.DEFAULT_RESOLVED_DOMAIN);
                        }
                        result.get(OUTCOME).set(SUCCESS);
                        result.get(RESULT);
                        return result;
                    }
                });
            }
        });
        builder.addChildResource(RemotingConnectorResource.REMOTE_CONNECTOR_CONFIG_PATH)
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT);

    }

    private void buildTransformers1_1_0(ResourceTransformationDescriptionBuilder builder) {
        rejectDefinedCoreMBeansSensitivity(builder);
        registerRejectAuditLogTransformers(builder);
    }

    private void rejectDefinedCoreMBeansSensitivity(ResourceTransformationDescriptionBuilder builder) {
        builder.getAttributeBuilder()
            .setDiscard(new DiscardAttributeValueChecker(new ModelNode(false)), JMXSubsystemRootResource.CORE_MBEAN_SENSITIVITY)
            .addRejectCheck(RejectAttributeChecker.DEFINED, JMXSubsystemRootResource.CORE_MBEAN_SENSITIVITY);
    }

    /**
     * Discards the audit log child as long as enabled==false. If enabled==true or enabled is undefined it rejects it.
     */
    private void registerRejectAuditLogTransformers(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder loggerBuilder = builder.addChildResource(JmxAuditLoggerResourceDefinition.PATH_ELEMENT);
        loggerBuilder.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, JmxAuditLoggerResourceDefinition.ENABLED)
            .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {
                public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                    return JmxLogger.ROOT_LOGGER.auditLogEnabledMustBeFalse();
                }

                protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                        TransformationContext context) {
                    //undefined defaults to true
                    return !attributeValue.isDefined() || attributeValue.asString().equals("true");
                }
            }, JmxAuditLoggerResourceDefinition.ENABLED);
        loggerBuilder.setCustomResourceTransformer(ResourceTransformer.DISCARD);
        builder.rejectChildResource(JmxAuditLoggerResourceDefinition.PATH_ELEMENT);
        loggerBuilder.setCustomResourceTransformer(ResourceTransformer.DISCARD);
        loggerBuilder.addOperationTransformationOverride(ADD).inheritResourceAttributeDefinitions().setCustomOperationTransformer(OperationTransformer.DISCARD);
        loggerBuilder.addOperationTransformationOverride(REMOVE).inheritResourceAttributeDefinitions().setCustomOperationTransformer(OperationTransformer.DISCARD);
        loggerBuilder.addOperationTransformationOverride(UNDEFINE_ATTRIBUTE_OPERATION).inheritResourceAttributeDefinitions().setCustomOperationTransformer(new OperationTransformer() {
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                    throws OperationFailedException {
                if (operation.require(ModelDescriptionConstants.NAME).asString().equals(JmxAuditLoggerResourceDefinition.ENABLED.getName())){
                    OperationRejectionPolicy rejectPolicy = new OperationRejectionPolicy() {
                        @Override
                        public boolean rejectOperation(ModelNode preparedResult) {
                            // Reject successful operations
                            return true;
                        }

                        @Override
                        public String getFailureDescription() {
                            return JmxLogger.ROOT_LOGGER.auditLogEnabledMustBeFalse();
                        }
                    };
                    return new TransformedOperation(null, rejectPolicy, OperationResultTransformer.ORIGINAL_RESULT);
                }
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
        });
        loggerBuilder.addOperationTransformationOverride(WRITE_ATTRIBUTE_OPERATION).inheritResourceAttributeDefinitions().setCustomOperationTransformer(OperationTransformer.DISCARD);
        loggerBuilder.addOperationTransformationOverride(READ_ATTRIBUTE_OPERATION).inheritResourceAttributeDefinitions().setCustomOperationTransformer(OperationTransformer.DISCARD);

        loggerBuilder.discardChildResource(JmxAuditLogHandlerReferenceResourceDefinition.PATH_ELEMENT);
    }

    private static class ResolvedOperationTransformer implements OperationTransformer {
        private final boolean showModel;

        public ResolvedOperationTransformer(boolean showModel) {
            this.showModel = showModel;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                throws OperationFailedException {
            PathAddress pathAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
            pathAddress = pathAddress.subAddress(0, pathAddress.size() - 1);
            ModelNode op = Util.getWriteAttributeOperation(pathAddress, CommonAttributes.SHOW_MODEL, new ModelNode(showModel));
            return new TransformedOperation(op,OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

    private static class JMXSubsystemParser_1_0 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);
            list.add(createAddOperation());

            boolean gotConnector = false;

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case JMX_CONNECTOR: {
                        if (gotConnector) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.JMX_CONNECTOR.getLocalName());
                        }
                        parseConnector(reader);
                        gotConnector = true;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        void parseConnector(XMLExtendedStreamReader reader) throws XMLStreamException {
            JmxLogger.ROOT_LOGGER.jmxConnectorNotSupported();
            String serverBinding = null;
            String registryBinding = null;
            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case SERVER_BINDING: {
                        serverBinding = value;
                        break;
                    }
                    case REGISTRY_BINDING: {
                        registryBinding = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            // Require no content
            ParseUtils.requireNoContent(reader);
            if (serverBinding == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.SERVER_BINDING));
            }
            if (registryBinding == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.REGISTRY_BINDING));
            }
        }
    }

    private static class JMXSubsystemParser_1_1 implements XMLStreamConstants, XMLElementReader<List<ModelNode>> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            boolean showModel = false;

            ParseUtils.requireNoAttributes(reader);

            ModelNode connectorAdd = null;
            list.add(createAddOperation());
            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case SHOW_MODEL:
                        if (showModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.SHOW_MODEL.getLocalName());
                        }
                        if (parseShowModelElement(reader)) {
                            //Add the show-model=>resolved part with the default domain name
                            ModelNode op = createOperation(ADD, CommonAttributes.EXPOSE_MODEL, CommonAttributes.RESOLVED);
                            //Use false here to keep total backwards compatibility
                            op.get(CommonAttributes.PROPER_PROPERTY_FORMAT).set(false);
                            list.add(op);
                        }
                        showModel = true;
                        break;
                    case REMOTING_CONNECTOR: {
                        if (connectorAdd != null) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.REMOTING_CONNECTOR.getLocalName());
                        }
                        list.add(parseRemoteConnector(reader));
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        protected ModelNode parseRemoteConnector(final XMLExtendedStreamReader reader) throws XMLStreamException {

            final ModelNode connector = new ModelNode();
            connector.get(OP).set(ADD);
            connector.get(OP_ADDR).add(SUBSYSTEM, JMX).add(REMOTING_CONNECTOR, CommonAttributes.JMX);

            int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case USE_MANAGEMENT_ENDPOINT: {
                        RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT.parseAndSetParameter(value, connector, reader);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }

            ParseUtils.requireNoContent(reader);
            return connector;
        }


        private boolean parseShowModelElement(XMLExtendedStreamReader reader) throws XMLStreamException {
            ParseUtils.requireSingleAttribute(reader, CommonAttributes.VALUE);
            return ParseUtils.readBooleanAttributeElement(reader, CommonAttributes.VALUE);
        }
    }

    private static class JMXSubsystemParser_1_2 extends JMXSubsystemParser_1_1 {

        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            boolean showResolvedModel = false;
            boolean showExpressionModel = false;
            boolean connectorAdd = false;

            ParseUtils.requireNoAttributes(reader);

            list.add(createAddOperation());

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case EXPOSE_RESOLVED_MODEL:
                        if (showResolvedModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.EXPOSE_RESOLVED_MODEL.getLocalName());
                        }
                        showResolvedModel = true;
                        list.add(parseShowModelElement(reader, CommonAttributes.RESOLVED));
                        break;
                    case EXPOSE_EXPRESSION_MODEL:
                        if (showExpressionModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.EXPOSE_EXPRESSION_MODEL.getLocalName());
                        }
                        showExpressionModel = true;
                        list.add(parseShowModelElement(reader, CommonAttributes.EXPRESSION));
                        break;
                    case REMOTING_CONNECTOR:
                        if (connectorAdd) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.REMOTING_CONNECTOR.getLocalName());
                        }
                        connectorAdd = true;
                        list.add(parseRemoteConnector(reader));
                        break;
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        protected ModelNode parseShowModelElement(XMLExtendedStreamReader reader, String showModelChild) throws XMLStreamException {

            ModelNode op = createOperation(ADD, CommonAttributes.EXPOSE_MODEL, showModelChild);

            String domainName = null;
            Boolean properPropertyFormat = null;

            for (int i = 0; i < reader.getAttributeCount(); i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case DOMAIN_NAME:
                        ExposeModelResource.getDomainNameAttribute(showModelChild).parseAndSetParameter(value, op, reader);
                        break;
                    case PROPER_PROPETY_FORMAT:
                        if (showModelChild.equals(CommonAttributes.RESOLVED)) {
                            ExposeModelResourceResolved.PROPER_PROPERTY_FORMAT.parseAndSetParameter(value, op, reader);
                        } else {
                            throw ParseUtils.unexpectedAttribute(reader, i);
                        }
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            if (domainName == null && properPropertyFormat == null) {
                ParseUtils.requireNoContent(reader);
            }
            return op;
        }
    }

    private static class JMXSubsystemParser_1_3 extends JMXSubsystemParser_1_2 {
        @Override
        public void readElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            boolean showResolvedModel = false;
            boolean showExpressionModel = false;
            boolean connectorAdd = false;
            boolean auditLog = false;
            boolean sensitivity = false;

            ParseUtils.requireNoAttributes(reader);

            ModelNode add = createAddOperation();
            list.add(add);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case EXPOSE_RESOLVED_MODEL:
                        if (showResolvedModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.EXPOSE_RESOLVED_MODEL.getLocalName());
                        }
                        showResolvedModel = true;
                        list.add(parseShowModelElement(reader, CommonAttributes.RESOLVED));
                        break;
                    case EXPOSE_EXPRESSION_MODEL:
                        if (showExpressionModel) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.EXPOSE_EXPRESSION_MODEL.getLocalName());
                        }
                        showExpressionModel = true;
                        list.add(parseShowModelElement(reader, CommonAttributes.EXPRESSION));
                        break;
                    case REMOTING_CONNECTOR:
                        if (connectorAdd) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.REMOTING_CONNECTOR.getLocalName());
                        }
                        connectorAdd = true;
                        list.add(parseRemoteConnector(reader));
                        break;
                    case AUDIT_LOG:
                        if (auditLog) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.AUDIT_LOG.getLocalName());
                        }
                        auditLog = true;
                        parseAuditLogElement(reader, list);
                        break;
                    case SENSITIVITY:
                        if (sensitivity) {
                            throw ParseUtils.duplicateNamedElement(reader, Element.SENSITIVITY.getLocalName());
                        }
                        sensitivity = true;
                        parseSensitivity(add, reader);
                        break;
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        private void parseSensitivity(ModelNode add, XMLExtendedStreamReader reader) throws XMLStreamException {
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NON_CORE_MBEANS:
                        JMXSubsystemRootResource.CORE_MBEAN_SENSITIVITY.parseAndSetParameter(value, add, reader);
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            ParseUtils.requireNoContent(reader);
        }

        private void parseAuditLogElement(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            ModelNode op = createOperation(ADD, JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getKey(), JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getValue());

            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case LOG_BOOT: {
                        JmxAuditLoggerResourceDefinition.LOG_BOOT.parseAndSetParameter(value, op, reader);
                        break;
                    }
                    case LOG_READ_ONLY: {
                        JmxAuditLoggerResourceDefinition.LOG_READ_ONLY.parseAndSetParameter(value, op, reader);
                        break;
                    }
                    case ENABLED: {
                        JmxAuditLoggerResourceDefinition.ENABLED.parseAndSetParameter(value, op, reader);
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            list.add(op);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case HANDLERS:
                        parseAuditLogHandlers(reader, list);
                        break;
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        private void parseAuditLogHandlers(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
            ParseUtils.requireNoAttributes(reader);

            while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                    case HANDLER:
                        parseAuditLogHandler(reader, list);
                        break;
                    default: {
                        throw ParseUtils.unexpectedElement(reader);
                    }
                }
            }
        }

        private void parseAuditLogHandler(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {

            String name = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME:
                        name = value;
                        break;
                    default:
                        throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }

            if (name == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(NAME));
            }
            ModelNode op = createOperation(ADD,
                    JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getKey(),
                    JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getValue(),
                    JmxAuditLogHandlerReferenceResourceDefinition.PATH_ELEMENT.getKey(), name);
            list.add(op);

            ParseUtils.requireNoContent(reader);
        }
    }


    private static class JMXSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {
        /**
         * {@inheritDoc}
         */
        @Override
        public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
            Namespace schemaVer = Namespace.CURRENT;
            final ModelNode node = context.getModelNode();

            context.startSubsystemElement(schemaVer.getUriString(), false);
            if (node.hasDefined(CommonAttributes.EXPOSE_MODEL)) {
                ModelNode showModel = node.get(CommonAttributes.EXPOSE_MODEL);
                if (showModel.hasDefined(CommonAttributes.RESOLVED)) {
                    writer.writeEmptyElement(Element.EXPOSE_RESOLVED_MODEL.getLocalName());
                    ExposeModelResourceResolved.DOMAIN_NAME.marshallAsAttribute(showModel.get(CommonAttributes.RESOLVED), false, writer);
                    ExposeModelResourceResolved.PROPER_PROPERTY_FORMAT.marshallAsAttribute(showModel.get(CommonAttributes.RESOLVED), false, writer);
                }
                if (showModel.hasDefined(CommonAttributes.EXPRESSION)) {
                    writer.writeEmptyElement(Element.EXPOSE_EXPRESSION_MODEL.getLocalName());
                    ExposeModelResourceExpression.DOMAIN_NAME.marshallAsAttribute(showModel.get(CommonAttributes.EXPRESSION), false, writer);
                }
            }
            if (node.hasDefined(CommonAttributes.REMOTING_CONNECTOR)) {
                writer.writeStartElement(Element.REMOTING_CONNECTOR.getLocalName());
                final ModelNode resourceModel = node.get(CommonAttributes.REMOTING_CONNECTOR).get(CommonAttributes.JMX);
                RemotingConnectorResource.USE_MANAGEMENT_ENDPOINT.marshallAsAttribute(resourceModel, writer);
                writer.writeEndElement();
            }

            if (node.hasDefined(JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getKey()) &&
                    node.get(JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getKey()).hasDefined(JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getValue())) {
                ModelNode auditLog = node.get(JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getKey(), JmxAuditLoggerResourceDefinition.PATH_ELEMENT.getValue());
                writer.writeStartElement(Element.AUDIT_LOG.getLocalName());
                JmxAuditLoggerResourceDefinition.LOG_BOOT.marshallAsAttribute(auditLog, writer);
                JmxAuditLoggerResourceDefinition.LOG_READ_ONLY.marshallAsAttribute(auditLog, writer);
                JmxAuditLoggerResourceDefinition.ENABLED.marshallAsAttribute(auditLog, writer);

                if (auditLog.hasDefined(HANDLER) && auditLog.get(HANDLER).keys().size() > 0) {
                    writer.writeStartElement(CommonAttributes.HANDLERS);
                    for (String key : auditLog.get(HANDLER).keys()) {
                        writer.writeEmptyElement(CommonAttributes.HANDLER);
                        writer.writeAttribute(CommonAttributes.NAME, key);
                    }
                    writer.writeEndElement();
                }

                writer.writeEndElement();
            }
            if (node.hasDefined(JMXSubsystemRootResource.CORE_MBEAN_SENSITIVITY.getName())) {
                writer.writeStartElement(Element.SENSITIVITY.getLocalName());
                JMXSubsystemRootResource.CORE_MBEAN_SENSITIVITY.marshallAsAttribute(node, writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

}
