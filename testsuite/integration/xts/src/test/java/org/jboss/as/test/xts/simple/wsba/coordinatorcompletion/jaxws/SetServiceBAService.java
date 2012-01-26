/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package org.jboss.as.test.xts.simple.wsba.coordinatorcompletion.jaxws;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.logging.Logger;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.2-hudson-182-RC1 Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "SetServiceBAService", targetNamespace = "http://www.jboss.com/jbossas/test/xts/simple/wsba/coordinatorcompletion")
public class SetServiceBAService extends Service {

    private final static URL SETSERVICEBASERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(SetServiceBAService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = SetServiceBAService.class.getResource(".");
            url = new URL(baseUrl, "SetServiceBA.wsdl");
        } catch (MalformedURLException e) {
            logger.warn("Failed to create URL for the wsdl Location: 'SetServiceBA.wsdl', retrying as a local file");
            logger.warn(e.getMessage());
        }
        SETSERVICEBASERVICE_WSDL_LOCATION = url;
    }

    public SetServiceBAService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SetServiceBAService() {
        super(SETSERVICEBASERVICE_WSDL_LOCATION, new QName("http://www.jboss.com/jbosstm/xts/quickstarts/Set",
                "SetServiceBAService"));
    }

    /**
     * 
     * @return returns ISetServiceBA
     */
    @WebEndpoint(name = "SetServiceBA")
    public SetServiceBA getSetServiceBA() {
        return super.getPort(new QName("http://www.jboss.com/jbosstm/xts/quickstarts/Set", "SetServiceBA"), SetServiceBA.class);
    }

}
