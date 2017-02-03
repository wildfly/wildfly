/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequired;
import static org.jboss.as.controller.parsing.ParseUtils.missingRequiredElement;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoContent;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoNamespaceAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedAttribute;
import static org.jboss.as.controller.parsing.ParseUtils.unexpectedElement;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.IDENTITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.SERVICE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.wildfly.common.Assert;

import org.wildfly.client.config.ConfigXMLParseException;

import org.wildfly.discovery.ServiceURL;

import java.util.EnumSet;

/**
 * Parser for ejb3:5.0 namespace.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class EJB3Subsystem50Parser extends EJB3Subsystem40Parser {

    EJB3Subsystem50Parser() {
    }

    @Override
    protected EJB3SubsystemNamespace getExpectedNamespace() {
        return EJB3SubsystemNamespace.EJB3_5_0;
    }

    @Override
    protected void readElement(final XMLExtendedStreamReader reader, final EJB3SubsystemXMLElement element, final List<ModelNode> operations, final ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        switch (element) {
            case APPLICATION_SECURITY_DOMAINS: {
                parseApplicationSecurityDomains(reader, operations);
                break;
            }
            case IDENTITY: {
                parseIdentity(reader, operations);
                break;
            }
            case ALLOW_EJB_NAME_REGEX: {
                parseAllowEjbNameRegex(reader, ejb3SubsystemAddOperation);
                break;
            }
            default: {
                super.readElement(reader, element, operations, ejb3SubsystemAddOperation);
            }
        }
    }

    private void parseApplicationSecurityDomains(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        requireNoAttributes(reader);
        boolean applicationSecurityDomainFound = false;
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case APPLICATION_SECURITY_DOMAIN: {
                    parseApplicationSecurityDomain(reader, operations);
                    applicationSecurityDomainFound = true;
                    break;
                }
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (! applicationSecurityDomainFound) {
            throw missingRequiredElement(reader, Collections.singleton(EJB3SubsystemXMLElement.APPLICATION_SECURITY_DOMAIN.getLocalName()));
        }
    }

    private void parseApplicationSecurityDomain(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        String applicationSecurityDomain = null;
        ModelNode operation = Util.createAddOperation();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String attributeValue = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    applicationSecurityDomain = attributeValue;
                    break;
                case SECURITY_DOMAIN:
                    ApplicationSecurityDomainDefinition.SECURITY_DOMAIN.parseAndSetParameter(attributeValue, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        if (applicationSecurityDomain == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }
        requireNoContent(reader);
        final PathAddress address = this.getEJB3SubsystemAddress().append(PathElement.pathElement(APPLICATION_SECURITY_DOMAIN, applicationSecurityDomain));
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);
    }

    private void parseAllowEjbNameRegex(XMLExtendedStreamReader reader, ModelNode ejb3SubsystemAddOperation) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        final EnumSet<EJB3SubsystemXMLAttribute> missingRequiredAttributes = EnumSet.of(EJB3SubsystemXMLAttribute.VALUE);
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case VALUE:
                    EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX.parseAndSetParameter(value, ejb3SubsystemAddOperation, reader);
                    // found the mandatory attribute
                    missingRequiredAttributes.remove(EJB3SubsystemXMLAttribute.VALUE);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }
        requireNoContent(reader);
        if (!missingRequiredAttributes.isEmpty()) {
            throw missingRequired(reader, missingRequiredAttributes);
        }
    }

    private void parseIdentity(final XMLExtendedStreamReader reader, final List<ModelNode> operations) throws XMLStreamException {
        final PathAddress address = this.getEJB3SubsystemAddress().append(SERVICE, IDENTITY);
        ModelNode addIdentity = Util.createAddOperation(address);
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case OUTFLOW_SECURITY_DOMAINS: {
                    for (String outflowDomain : reader.getListAttributeValue(i)) {
                        IdentityResourceDefinition.OUTFLOW_SECURITY_DOMAINS.parseAndAddParameterElement(outflowDomain, addIdentity, reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedAttribute(reader, i);
                }
            }

        }
        requireNoContent(reader);
        operations.add(addIdentity);
    }

    protected void parseProfile(final XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        String profileName = null;

        final EJBClientContext.Builder builder = new EJBClientContext.Builder();

        final ModelNode operation = Util.createAddOperation();

        for (int i = 0; i < count; i++) {
            requireNoNamespaceAttribute(reader, i);
            final String value = reader.getAttributeValue(i);
            final EJB3SubsystemXMLAttribute attribute = EJB3SubsystemXMLAttribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME:
                    profileName = value;
                    break;
                case EXCLUDE_LOCAL_RECEIVER:
                    RemotingProfileResourceDefinition.EXCLUDE_LOCAL_RECEIVER.parseAndSetParameter(value, operation, reader);
                    break;
                case LOCAL_RECEIVER_PASS_BY_VALUE:
                    RemotingProfileResourceDefinition.LOCAL_RECEIVER_PASS_BY_VALUE.parseAndSetParameter(value, operation, reader);
                    break;
                default:
                    throw unexpectedAttribute(reader, i);
            }
        }

        if (profileName == null) {
            throw missingRequired(reader, Collections.singleton(EJB3SubsystemXMLAttribute.NAME.getLocalName()));
        }

        final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.REMOTING_PROFILE, profileName);
        operation.get(OP_ADDR).set(address.toModelNode());
        operations.add(operation);

        List<ServiceURL> staticURLs = new ArrayList<>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case DISCOVERY:
                    parseDiscoveryType(reader, profileName, operations);
                    break;
                case GLOBAL_INTERCEPTORS:
                    parseInterceptorsType(reader, builder);
                    break;
                case CONNECTIONS:
                    parseConnectionsType(reader, builder);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (!staticURLs.isEmpty()) {
            //TODO Elytron - quickfix after discovery update required
        }
    }

    private void parseDiscoveryType(final XMLExtendedStreamReader reader, final String profileName, final List<ModelNode> operations) throws XMLStreamException {
        if (reader.getAttributeCount() > 0) {
            requireNoAttributes(reader);
        }
        List<ModelNode> staticURLs = new ArrayList<>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case STATIC_EJB_DISCOVERY:
                    parseStaticEjbDiscoveryType(reader, staticURLs);
                    break;
                case STATIC_CLUSTER_DISCOVERY:
                    parseStaticClusterDiscoveryType(reader, staticURLs);
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }
        if (staticURLs != null) {
            final ModelNode operation = Util.createAddOperation();
            final PathAddress address = SUBSYSTEM_PATH.append(EJB3SubsystemModel.REMOTING_PROFILE, profileName).append(EJB3SubsystemModel.DISCOVERY, EJB3SubsystemModel.STATIC);
            operation.get(OP_ADDR).set(address.toModelNode());
            operation.get(DiscoveryResourceDefinition.STATIC_URLS.getName()).set(staticURLs);
            operations.add(operation);
        }
    }



    private void parseStaticEjbDiscoveryType(final XMLExtendedStreamReader reader, final List<ModelNode> staticURLs) throws XMLStreamException {
        final int attributeCount = reader.getAttributeCount();
        String appName = null;
        String moduleName = null;
        String beanName = null;
        String distinctName = null;
        String uri = null;
        for (int i = 0; i < attributeCount; i ++) {
            requireNoNamespaceAttribute(reader, i);
            if (reader.getAttributeLocalName(i).equals("app-name")) {
                appName = reader.getAttributeValue(i);
            } else if (reader.getAttributeLocalName(i).equals("module-name")) {
                moduleName = reader.getAttributeValue(i);
            } else if (reader.getAttributeLocalName(i).equals("bean-name")) {
                beanName = reader.getAttributeValue(i);
            } else if (reader.getAttributeLocalName(i).equals("distinct-name")) {
                distinctName = reader.getAttributeValue(i);
            } else if (reader.getAttributeLocalName(i).equals("uri")) {
                uri = reader.getAttributeValue(i);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        if (moduleName == null) {
            throw missingRequired(reader, "module-name");
        } else if (beanName == null) {
            throw missingRequired(reader, "bean-name");
        } else if (uri == null) {
            throw missingRequired(reader, "uri");
        }

        String primary;
        if (appName == null) {
            primary = "/" + moduleName;
        } else {
            primary = appName + "/" + moduleName;
        }
        String secondary;
        if (distinctName == null) {
            secondary = beanName;
        } else {
            secondary = beanName + "/" + distinctName;
        }
        final ModelNode attributesNode = new ModelNode();
        attributesNode.get("ejb-bean").set(primary+ "/" + secondary);
        attributesNode.get("uri").set(uri);

        final ModelNode urlNode = new ModelNode();
        urlNode.get(DiscoveryResourceDefinition.URL_ATTRIBUTES.getName()).set(attributesNode);
        urlNode.get(DiscoveryResourceDefinition.ABSTRACT_TYPE.getName()).set("ejb");
        urlNode.get(DiscoveryResourceDefinition.ABSTRACT_TYPE_AUTHORITY.getName()).set("jboss");
        staticURLs.add(urlNode);
        if (reader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(reader);
        }
    }

    private void parseStaticClusterDiscoveryType(final XMLExtendedStreamReader reader, final List<ModelNode> staticURLs) throws XMLStreamException {
        final int attributeCount = reader.getAttributeCount();
        String clusterName = null;
        for (int i = 0; i < attributeCount; i ++) {
            requireNoNamespaceAttribute(reader, i);
            if (reader.getAttributeLocalName(i).equals("cluster-name")) {
                clusterName = reader.getAttributeValue(i);
            } else {
                throw unexpectedAttribute(reader, i);
            }
        }
        if (clusterName == null) {
            throw missingRequired(reader, "cluster-name");
        }

        final List<String> connectUris = new ArrayList<>();
        while (reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT) {
            switch (EJB3SubsystemXMLElement.forName(reader.getLocalName())) {
                case CONNECT_TO:
                    connectUris.add(parseConnectTo(reader));
                    break;
                default: {
                    throw unexpectedElement(reader);
                }
            }
        }

        if (connectUris != null) for (String connectUri : connectUris) {
            final ModelNode urlNode = new ModelNode();
            final ModelNode attributesNode = new ModelNode();
            attributesNode.get("cluster-name").set(clusterName);
            attributesNode.get("uri").set(connectUri);
            urlNode.get(DiscoveryResourceDefinition.URL_ATTRIBUTES.getName()).set(attributesNode);
            urlNode.get(DiscoveryResourceDefinition.ABSTRACT_TYPE.getName()).set("ejb");
            urlNode.get(DiscoveryResourceDefinition.ABSTRACT_TYPE_AUTHORITY.getName()).set("jboss");
            staticURLs.add(urlNode);
        }
    }

    private String parseConnectTo(final XMLExtendedStreamReader streamReader) throws XMLStreamException {
        final int attributeCount = streamReader.getAttributeCount();
        String uri = null;
        for (int i = 0; i < attributeCount; i ++) {
            requireNoNamespaceAttribute(streamReader, i);
            if (!streamReader.getAttributeLocalName(i).equals("uri")) {
                throw unexpectedAttribute(streamReader, i);
            }
            uri = streamReader.getAttributeValue(i);
        }
        if (uri == null) {
            throw missingRequired(streamReader, "uri");
        }
        if (streamReader.nextTag() != END_ELEMENT) {
            throw unexpectedElement(streamReader);
        }
        return uri;
    }

    private void parseInterceptorsType(final XMLExtendedStreamReader streamReader, final EJBClientContext.Builder builder) throws XMLStreamException {
        if (streamReader.getAttributeCount() > 0) {
            throw unexpectedAttribute(streamReader, 0);
        }
        for (;;) {
            final int next = streamReader.nextTag();
            if (next == START_ELEMENT) {
                parseInterceptorType(streamReader, builder);
            } else if (next == END_ELEMENT) {
                return;
            } else {
                throw Assert.unreachableCode();
            }
        }
    }

    private void parseInterceptorType(final XMLExtendedStreamReader streamReader, final EJBClientContext.Builder builder) throws XMLStreamException {
        final int attributeCount = streamReader.getAttributeCount();
        String className = null;
        String moduleName = null;
        for (int i = 0; i < attributeCount; i++) {
            if (streamReader.getNamespaceURI(i) != null) {
                throw unexpectedAttribute(streamReader, i);
            }
            final String name = streamReader.getAttributeLocalName(i);
            if (name.equals("class")) {
                className = streamReader.getAttributeValue(i);
            } else if (name.equals("moduleName")) {
                moduleName = streamReader.getAttributeValue(i);
            } else {
                throw unexpectedAttribute(streamReader, i);
            }
        }
        if (className == null) {
            throw missingRequired(streamReader, "class");
        }
        ClassLoader cl;
        if (moduleName != null) {
            try {
                cl = Module.getModuleFromCallerModuleLoader(ModuleIdentifier.fromString(moduleName)).getClassLoader();
            } catch (ModuleLoadException e) {
                throw new ConfigXMLParseException(e);
            }
        } else {
            cl = EJB3Subsystem50Parser.class.getClassLoader();
        }
        final Class<? extends EJBClientInterceptor> interceptorClass;
        final EJBClientInterceptor interceptor;
        try {
            interceptorClass = Class.forName(className, false, cl).asSubclass(EJBClientInterceptor.class);
            interceptor = interceptorClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
            throw new ConfigXMLParseException(e);
        }
        builder.addInterceptor(interceptor);
        final int next = streamReader.nextTag();
        if (next == END_ELEMENT) {
            return;
        }
        throw unexpectedElement(streamReader);
    }

    private void parseConnectionsType(final XMLExtendedStreamReader streamReader, final EJBClientContext.Builder builder) throws XMLStreamException {
        if (streamReader.getAttributeCount() > 0) {
            requireNoAttributes(streamReader);
        }
        for (;;) {
            final int next = streamReader.nextTag();
            if (next == START_ELEMENT) {
                final String localName = streamReader.getLocalName();
                if (localName.equals("connection")) {
                    parseConnectionType(streamReader, builder);
                } else {
                    throw unexpectedElement(streamReader);
                }
            } else if (next == END_ELEMENT) {
                return;
            } else {
                throw Assert.unreachableCode();
            }
        }
    }

    private void parseConnectionType(final XMLExtendedStreamReader streamReader, final EJBClientContext.Builder builder) throws XMLStreamException {
        URI uri = null;
        final int attributeCount = streamReader.getAttributeCount();
        for (int i = 0; i < attributeCount; i ++) {
            if (streamReader.getNamespaceURI(i) != null || ! streamReader.getAttributeLocalName(i).equals("uri") || uri != null) {
                throw unexpectedAttribute(streamReader, i);
            }
            uri = getURIAttributeValue(streamReader, i);
        }
        if (uri == null) {
            throw missingRequired(streamReader, "uri");
        }
        final int next = streamReader.nextTag();
        if (next == START_ELEMENT) {
            final String localName = streamReader.getLocalName();
            if (localName.equals("interceptors")) {
                // todo...
                skipContent(streamReader);
            }
        } else if (next == END_ELEMENT) {
            return;
        } else {
            throw Assert.unreachableCode();
        }
    }

    private URI getURIAttributeValue(final XMLExtendedStreamReader streamReader, final int i) throws XMLStreamException {
        try {
            return new URI(streamReader.getAttributeValue(i));
        } catch(URISyntaxException ex){
            throw new XMLStreamException(ex);
        }
    }

    private void skipContent(final XMLExtendedStreamReader streamReader) throws XMLStreamException {
        while (streamReader.hasNext()) {
            switch (streamReader.next()) {
                case START_ELEMENT: {
                    skipContent(streamReader);
                    break;
                }
                case END_ELEMENT: {
                    return;
                }
            }
        }
    }

}
