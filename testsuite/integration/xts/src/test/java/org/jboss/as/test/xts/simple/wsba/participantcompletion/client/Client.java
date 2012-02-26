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
package org.jboss.as.test.xts.simple.wsba.participantcompletion.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;

import com.arjuna.mw.wst11.client.JaxWSHeaderContextProcessor;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.xts.simple.wsba.AlreadyInSetException;
import org.jboss.as.test.xts.simple.wsba.SetServiceException;
import org.jboss.as.test.xts.simple.wsba.participantcompletion.jaxws.SetServiceBA;

/**
 * A Client stub to the SetService.
 *
 * @author paul.robinson@redhat.com, 2012-01-04
 */
@ClientStub
public class Client implements SetServiceBA {
    private SetServiceBA set;

    /**
     * Default constructor with hard-coded values for the SetService endpoint details (wsdl url, service name & port name)
     *
     * @throws java.net.MalformedURLException if the WSDL url is malformed.
     */
    public Client() throws MalformedURLException {
        String node0 = TestSuiteEnvironment.getServerAddress();

        URL wsdlLocation = new URL("http://" + node0 + ":8080/" + WSBAParticipantCompletionTestCase.DEPLOYMENT_NAME + "/SetServiceBA?wsdl");
        QName serviceName = new QName("http://www.jboss.com/jbossas/test/xts/simple/wsba/participantcompletion",
                "SetServiceBAService");
        QName portName = new QName("http://www.jboss.com/jbossas/test/xts/simple/wsba/participantcompletion",
                "SetServiceBA");

        Service service = Service.create(wsdlLocation, serviceName);
        set = service.getPort(portName, SetServiceBA.class);

        /*
         * Add client handler chain so that XTS can add the transaction context to the SOAP messages.
         */
        BindingProvider bindingProvider = (BindingProvider) set;
        List<Handler> handlers = new ArrayList<Handler>(1);
        handlers.add(new JaxWSHeaderContextProcessor());
        bindingProvider.getBinding().setHandlerChain(handlers);
    }

    /**
     * Add a value to the set
     *
     * @param value Value to add to the set.
     * @throws AlreadyInSetException if the item is already in the set.
     * @throws SetServiceException if an error occurred during the adding of the item to the set.
     */
    public void addValueToSet(String value) throws AlreadyInSetException, SetServiceException {
        set.addValueToSet(value);
    }

    /**
     * Query the set to see if it contains a particular value.
     *
     * @param value the value to check for.
     * @return true if the value was present, false otherwise.
     */
    public boolean isInSet(String value) {
        return set.isInSet(value);
    }

    /**
     * Empty the set
     */
    public void clear() {
        set.clear();
    }
}
