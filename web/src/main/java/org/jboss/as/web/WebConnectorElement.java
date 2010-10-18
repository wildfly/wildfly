/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * The web connector configuration element.
 *
 * @author Emanuel Muckenhuber
 */
public class WebConnectorElement extends AbstractModelElement<WebConnectorElement> {

    /** The serialVersionUID */
    private static final long serialVersionUID = 4884228320917906019L;

    private final String name;
    private String protocol;
    private String socketBinding;
    private String scheme;
    private String executorRef;
    private Boolean enabled;
    private Boolean enableLookups;
    private String proxyName;
    private Integer proxyPort;
    private Integer redirectPort;
    private Boolean secure;
    private Integer maxPostSize;
    private Integer maxSavePostSize;

    protected WebConnectorElement(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null connector name");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getBindingRef() {
        return socketBinding;
    }

    void setBindingRef(String bindingRef) {
        this.socketBinding = bindingRef;
    }

    public String getExecutorRef() {
        return executorRef;
    }

    void setExecutorRef(String executorRef) {
        this.executorRef = executorRef;
    }

    public String getScheme() {
        return scheme;
    }

    void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public Boolean isEnableLookups() {
        return enableLookups;
    }

    public void setEnableLookups(Boolean enableLookups) {
        this.enableLookups = enableLookups;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public Integer getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Integer getRedirectPort() {
        return redirectPort;
    }

    public void setRedirectPort(Integer redirectPort) {
        this.redirectPort = redirectPort;
    }

    public Integer getMaxPostSize() {
        return maxPostSize;
    }

    public void setMaxPostSize(Integer maxPostSize) {
        this.maxPostSize = maxPostSize;
    }

    public Integer getMaxSavePostSize() {
        return maxSavePostSize;
    }

    public void setMaxSavePostSize(Integer maxSavePostSize) {
        this.maxSavePostSize = maxSavePostSize;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /** {@inheritDoc} */
    protected Class<WebConnectorElement> getElementClass() {
        return WebConnectorElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);
        writeAttribute(Attribute.PROTOCOL, protocol, streamWriter);
        writeAttribute(Attribute.SOCKET_BINDING, socketBinding, streamWriter);
        writeAttribute(Attribute.SCHEME, scheme, streamWriter);
        writeAttribute(Attribute.EXECUTOR, executorRef, streamWriter);
        if (enabled != null) {
            writeAttribute(Attribute.ENABLED, enabled.toString(), streamWriter);
        }
        if(secure != null) {
            writeAttribute(Attribute.SECURE, secure.toString(), streamWriter);
        }
        if(enableLookups != null) {
            writeAttribute(Attribute.ENABLE_LOOKUPS, enableLookups.toString(), streamWriter);
        }
        writeAttribute(Attribute.PROXY_NAME, proxyName, streamWriter);
        if(proxyPort != null) {
            writeAttribute(Attribute.PROXY_PORT, proxyPort.toString(), streamWriter);
        }
        if(redirectPort != null) {
            writeAttribute(Attribute.REDIRECT_PORT, redirectPort.toString(), streamWriter);
        }
        if(maxPostSize != null) {
            writeAttribute(Attribute.MAX_POST_SIZE, maxPostSize.toString(), streamWriter);
        }
        if(maxSavePostSize != null) {
            writeAttribute(Attribute.MAX_SAVE_POST_SIZE, maxSavePostSize.toString(), streamWriter);
        }
        streamWriter.writeEndElement();
    }

    static void writeAttribute(final Attribute attribute, String content, XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(content != null) {
            streamWriter.writeAttribute(attribute.getLocalName(), content);
        }
    }

}
