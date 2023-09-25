/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.xerces.ws;

import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
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
