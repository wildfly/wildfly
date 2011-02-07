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
package org.jboss.as.webservices;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Nov-2010
 *
 */
public class WSConfigurationElement extends AbstractModelElement<WSConfigurationElement> {

    private static final long serialVersionUID = 5817061097201118815L;

    private String webServiceHost;
    private boolean modifySOAPAddress;
    private Integer webServicePort;
    private Integer webServiceSecurePort;

    public String getWebServiceHost() {
        return webServiceHost;
    }
    public void setWebServiceHost(String webServiceHost) {
        this.webServiceHost = webServiceHost;
    }
    public boolean isModifySOAPAddress() {
        return modifySOAPAddress;
    }
    public void setModifySOAPAddress(boolean modifySOAPAddress) {
        this.modifySOAPAddress = modifySOAPAddress;
    }
    public Integer getWebServicePort() {
        return webServicePort;
    }
    public void setWebServicePort(Integer webServicePort) {
        this.webServicePort = webServicePort;
    }
    public Integer getWebServiceSecurePort() {
        return webServiceSecurePort;
    }
    public void setWebServiceSecurePort(Integer webServiceSecurePort) {
        this.webServiceSecurePort = webServiceSecurePort;
    }
    @Override
    protected Class<WSConfigurationElement> getElementClass() {
        return WSConfigurationElement.class;
    }
    @Override
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeStartElement(Element.WEBSERVICE_HOST.getLocalName());
        streamWriter.writeCharacters(webServiceHost);
        streamWriter.writeEndElement();

        streamWriter.writeStartElement(Element.MODIFY_SOAP_ADDRESS.getLocalName());
        streamWriter.writeCharacters(String.valueOf(isModifySOAPAddress()));
        streamWriter.writeEndElement();

        if (webServiceSecurePort != null) {
            streamWriter.writeStartElement(Element.WEBSERVICE_SECURE_PORT.getLocalName());
            streamWriter.writeCharacters(String.valueOf(webServiceSecurePort));
            streamWriter.writeEndElement();
        }

        if (webServicePort != null) {
            streamWriter.writeStartElement(Element.WEBSERVICE_PORT.getLocalName());
            streamWriter.writeCharacters(String.valueOf(webServicePort));
            streamWriter.writeEndElement();
        }
    }
}
