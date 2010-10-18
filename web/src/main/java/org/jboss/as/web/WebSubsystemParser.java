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

package org.jboss.as.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.ExtensionContext;
import org.jboss.as.ExtensionContext.SubsystemConfiguration;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.ParseResult;
import org.jboss.as.model.ParseUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * The web subsystem parser.
 *
 * @author Emanuel Muckenhuber
 */
public class WebSubsystemParser implements XMLStreamConstants, XMLElementReader<ParseResult<ExtensionContext.SubsystemConfiguration<WebSubsystemElement>>> {

    private static final WebSubsystemParser INSTANCE = new WebSubsystemParser();

    public static WebSubsystemParser getInstance() {
        return INSTANCE;
    }

    private WebSubsystemParser() {
        //
    }

    /** {@inheritDoc} */
    public void readElement(XMLExtendedStreamReader reader, ParseResult<SubsystemConfiguration<WebSubsystemElement>> result) throws XMLStreamException {
        final List<AbstractSubsystemUpdate<WebSubsystemElement, ?>> updates = new ArrayList<AbstractSubsystemUpdate<WebSubsystemElement,?>>();
        final WebSubsystemAdd subsystem = new WebSubsystemAdd();
        // no attributes
        if (reader.getAttributeCount() > 0) {
            throw ParseUtils.unexpectedAttribute(reader, 0);
        }
        // elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case WEB_1_0: {
                    final Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case CONTAINER_CONFIG: {

                            break;
                        }
                        case CONNECTOR: {
                            parseConnector(reader, updates);
                            break;
                        }
                        case VIRTUAL_SERVER: {
                            parseHost(reader, updates);
                            break;
                        } default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                } default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        result.setResult(new ExtensionContext.SubsystemConfiguration<WebSubsystemElement>(subsystem, updates));
    }

    void parseConnector(final XMLExtendedStreamReader reader, final List<AbstractSubsystemUpdate<WebSubsystemElement, ?>> list) throws XMLStreamException {
        String name = null;
        String protocol = null;
        String bindingRef = null;
        String scheme = null;
        String executorRef = null;
        String enabled = null;
        String enableLookups = null;
        String proxyName = null;
        String proxyPort = null;
        String maxPostSize = null;
        String maxSavePostSize = null;
        String secure = null;
        String redirectPort = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case NAME:
                    name = value;
                    break;
                case SOCKET_BINDING:
                    bindingRef = value;
                    break;
                case SCHEME:
                    scheme = value;
                    break;
                case PROTOCOL:
                    protocol = value;
                    break;
                case EXECUTOR:
                    executorRef = value;
                    break;
                case ENABLED:
                    enabled = value;
                    break;
                case ENABLE_LOOKUPS:
                    enableLookups = value;
                    break;
                case PROXY_NAME:
                    proxyName = value;
                    break;
                case PROXY_PORT:
                    proxyPort = value;
                    break;
                case MAX_POST_SIZE:
                    maxPostSize = value;
                    break;
                case MAX_SAVE_POST_SIZE:
                    maxSavePostSize = value;
                    break;
                case SECURE:
                    secure = value;
                    break;
                case REDIRECT_PORT:
                    redirectPort = value;
                    break;
                default:
                    ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        if (name == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        if (protocol == null) {
            ParseUtils.missingRequired(reader, Collections.singleton(Attribute.PROTOCOL));
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        final WebConnectorAdd action = new WebConnectorAdd(name);
        action.setBindingRef(bindingRef);
        action.setProtocol(protocol);
        action.setScheme(scheme);
        action.setExecutorRef(executorRef);
        action.setProxyName(proxyName);
        if(enabled != null) action.setEnabled(Boolean.valueOf(enabled));
        if(proxyPort != null) action.setProxyPort(Integer.valueOf(proxyPort));
        if(enableLookups != null) action.setEnableLookups(Boolean.valueOf(enableLookups));
        if(redirectPort != null) action.setRedirectPort(Integer.valueOf(redirectPort));
        if(secure != null) action.setSecure(Boolean.valueOf(secure));
        if(maxPostSize != null) action.setMaxPostSize(Integer.valueOf(maxPostSize));
        if(maxSavePostSize != null) action.setMaxSavePostSize(Integer.valueOf(maxSavePostSize));
        list.add(action);
    }

    static void parseHost(final XMLExtendedStreamReader reader, final List<AbstractSubsystemUpdate<WebSubsystemElement, ?>> list) throws XMLStreamException {
        String name = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        name = value;
                        break;
                    } default: {
                        ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
        }
        if(name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
        }
        Set<String> aliases = new HashSet<String>();
        WebHostAccessLogElement accessLog = null;
        WebHostRewriteElement rewrite = null;
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
            case WEB_1_0: {
                final Element element = Element.forName(reader.getLocalName());
                switch (element) {
                case ALIAS:
                    aliases.add(readSingleAttributeNoContent(reader));
                    break;
                case ACCESS_LOG:
                    accessLog = parseHostAccessLog(reader);
                    break;
                case REWRITE:
                    rewrite = parseHostRewrite(reader);
                    break;
                default:
                    throw ParseUtils.unexpectedElement(reader);
                }
                break;
            }
            default:
                throw ParseUtils.unexpectedElement(reader);
            }
        }
        final WebVirtualHostAdd action = new WebVirtualHostAdd(name);
        action.setAliases(aliases);
        action.setAccessLog(accessLog);
        action.setRewrite(rewrite);
        list.add(action);
    }

    static WebHostAccessLogElement parseHostAccessLog(final XMLExtendedStreamReader reader) throws XMLStreamException {
        String pattern = null;
        String prefix = null;
        Boolean rotate = null;
        Boolean extended = null;
        Boolean resolveHosts = null;
        WebHostAccessLogElement.LogDirectory directory = null;
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                case DIRECTORY:
                    directory = parseLogDir(reader);
                    if(directory.isEmpty()) {
                        directory = null;
                    }
                    break;
                case PATTERN:
                    pattern = value;
                    break;
                case PREFIX:
                    prefix = value;
                    break;
                case RESOLVE_HOSTS:
                    resolveHosts = Boolean.valueOf(value);
                    break;
                case EXTENDED:
                    extended = Boolean.valueOf(value);
                    break;
                case ROTATE:
                    rotate = Boolean.valueOf(value);
                    break;
                default:
                    ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        ParseUtils.requireNoContent(reader);
        final WebHostAccessLogElement accessLog = new WebHostAccessLogElement();
        accessLog.setPattern(pattern);
        accessLog.setExtended(extended);
        accessLog.setPrefix(prefix);
        accessLog.setRotate(rotate);
        accessLog.setResolveHosts(resolveHosts);
        return accessLog;
    }

    static WebHostAccessLogElement.LogDirectory parseLogDir(XMLExtendedStreamReader reader) throws XMLStreamException {
        final WebHostAccessLogElement.LogDirectory directory = new WebHostAccessLogElement.LogDirectory();
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                throw ParseUtils.unexpectedAttribute(reader, i);
            } else {
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case RELATIVE_TO:
                        directory.setRelativeTo(value.trim());
                        break;
                    case PATH:
                        directory.setPath(value.trim());
                        break;
                    default:
                        ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }
        // Handle elements
        ParseUtils.requireNoContent(reader);
        return directory;
    }

    static WebHostRewriteElement parseHostRewrite(final XMLExtendedStreamReader reader) throws XMLStreamException {

        // FIXME

        ParseUtils.requireNoContent(reader);
        return new WebHostRewriteElement();
    }

    static String readSingleAttributeNoContent(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final int count = reader.getAttributeCount();
        if(count > 1) {
            throw ParseUtils.unexpectedAttribute(reader, 1);
        }
        final String value = reader.getAttributeValue(0);
        ParseUtils.requireNoContent(reader);
        return value.trim();
    }

}
