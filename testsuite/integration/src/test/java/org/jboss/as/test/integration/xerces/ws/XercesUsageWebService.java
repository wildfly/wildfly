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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.xerces.ws;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.InputStream;

/**
 * User: jpai
 */
@WebService (serviceName = "XercesUsageWebService", targetNamespace = "org.jboss.as.test.integration.xerces.ws")
@SOAPBinding
public class XercesUsageWebService implements XercesUsageWSEndpoint {

    public static final String SUCCESS_MESSAGE = "Success";

    @Override
    public String parseUsingXerces(String xmlResource) {
        final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(xmlResource);
        if (inputStream == null) {
            throw new RuntimeException(xmlResource + " could not be found");
        }
        DOMParser domParser = new DOMParser();
        try {
            domParser.parse(new InputSource(inputStream));
            return SUCCESS_MESSAGE;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
