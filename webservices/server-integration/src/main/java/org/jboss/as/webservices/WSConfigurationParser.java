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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Parser for the ws server configuration
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Nov-2010
 *
 */
public class WSConfigurationParser {

    public static WSConfigurationElement parse(XMLStreamReader reader) throws Exception {
        WSConfigurationElement configuration = null;
        int iterate;
        try {
            iterate = reader.nextTag();
        } catch (XMLStreamException e) {
            // skip non-tag elements
            iterate = reader.nextTag();
        }
        switch (iterate) {
            case XMLStreamConstants.END_ELEMENT: {
                // we're done
                break;
            }
            case XMLStreamConstants.START_ELEMENT: {

                switch (Element.forName(reader.getLocalName())) {
                    case CONFIGURATION: {
                        configuration = parseConfiguration(reader);
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unexpected element: " + reader.getLocalName());
                }
                break;
            }
            default:
                throw new IllegalStateException();
        }
        return configuration;
    }

    private static WSConfigurationElement parseConfiguration(XMLStreamReader reader) throws Exception {
        WSConfigurationElement configuration = new WSConfigurationElement();
        // elements reading
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    if (Element.forName(reader.getLocalName()) == Element.CONFIGURATION) {
                        return configuration;
                    } else {
                        if (Element.forName(reader.getLocalName()) == Element.UNKNOWN) {
                            throw new IllegalStateException("Unexpected end tag: " + reader.getLocalName());
                        }
                    }
                    break;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    switch (Element.forName(reader.getLocalName())) {
                        case MODIFY_SOAP_ADDRESS: {
                            configuration.setModifySOAPAddress(elementAsBoolean(reader));
                            break;
                        }
                        case WEBSERVICE_HOST: {
                            configuration.setWebServiceHost(elementAsString(reader));
                            break;
                        }
                        case WEBSERVICE_PORT: {
                            configuration.setWebServicePort(elementAsInteger(reader));
                            break;
                        }
                        case WEBSERVICE_SECURE_PORT: {
                            configuration.setWebServiceSecurePort(elementAsInteger(reader));
                            break;
                        }
                        default:
                            throw new IllegalStateException("Unexpected element: " + reader.getLocalName());
                    }
                    break;
                }
            }
        }
        throw new IllegalStateException("Reached end of xml document unexpectedly");
    }

    /**
     * convert an xml element in boolean value. Empty elements give true
     *
     * @param reader the StAX reader
     * @return the boolean representing element
     * @throws XMLStreamException StAX exception
     */
    protected static boolean elementAsBoolean(XMLStreamReader reader) throws XMLStreamException {
        String elementtext = reader.getElementText();
        return elementtext == null || elementtext.length() == 0 ? true : Boolean.valueOf(elementtext.trim());
    }

    /**
     * convert an xml element into a String value
     *
     * @param reader the StAX reader
     * @return the string representing element
     * @throws XMLStreamException StAX exception
     */
    protected static String elementAsString(XMLStreamReader reader) throws XMLStreamException {
        String elementtext = reader.getElementText();
        return elementtext == null ? null : elementtext.trim();
    }

    /**
     * convert an xml element into an Integer value
     *
     * @param reader the StAX reader
     * @return the integer representing element
     * @throws XMLStreamException StAX exception
     * @throws NumberFormatException in case it isn't a number
     */
    protected static Integer elementAsInteger(XMLStreamReader reader) throws XMLStreamException, NumberFormatException {
        String stringValue = elementAsString(reader);
        return stringValue == null || stringValue.length() == 0 ? null : Integer.valueOf(stringValue);
    }
}
