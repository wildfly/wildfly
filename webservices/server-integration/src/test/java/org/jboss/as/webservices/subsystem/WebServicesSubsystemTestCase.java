/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.subsystem;

import java.io.IOException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.as.webservices.dmr.WSExtension;
import org.junit.Ignore;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Ignore("AS7-1804")
public class WebServicesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public WebServicesSubsystemTestCase() {
        super(WSExtension.SUBSYSTEM_NAME, new WSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        //This is copied from standalone-preview.xml. Testing more combinations would be good
        return
            "<subsystem xmlns=\"urn:jboss:domain:webservices:1.0\">" +
            "    <modify-wsdl-address>true</modify-wsdl-address>" +
            "    <wsdl-host>localhost</wsdl-host>" +
            "    <!--" +
            "    <wsdl-port>8080</wsdl-port>" +
            "    <wsdl-secure-port>8443</wsdl-secure-port>" +
            "    -->" +
            "    <endpoint-config xmlns:ws=\"urn:jboss:jbossws-jaxws-config:4.0\">" +
            "       <ws:config-name>Standard-Endpoint-Config</ws:config-name>" +
            "    </endpoint-config>" +
            "    <endpoint-config xmlns:ws=\"urn:jboss:jbossws-jaxws-config:4.0\">" +
            "       <ws:config-name>Recording-Endpoint-Config</ws:config-name>" +
            "       <ws:pre-handler-chains>" +
            "           <handler-chain xmlns=\"http://java.sun.com/xml/ns/javaee\">" +
            "               <protocol-bindings>##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM</protocol-bindings>" +
            "               <handler>" +
            "                   <handler-name>RecordingHandler</handler-name>" +
            "                   <handler-class>org.jboss.ws.common.invocation.RecordingServerHandler</handler-class>" +
            "               </handler>" +
            "           </handler-chain>" +
            "       </ws:pre-handler-chains>" +
            "    </endpoint-config>" +
            "</subsystem>";

    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }


            @Override
            protected ValidationConfiguration getModelValidationConfiguration() {
                //TODO fix providers https://issues.jboss.org/browse/AS7-1804
                return null;
            }


            @Override
            protected boolean isValidateOperations() {
                //TODO fix providers https://issues.jboss.org/browse/AS7-1804
                return false;
            }
        };
    }

}
