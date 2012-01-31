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

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.webservices.dmr.WSExtension;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WebServicesSubsystemTestCase extends AbstractSubsystemBaseTest {

    public WebServicesSubsystemTestCase() {
        super(WSExtension.SUBSYSTEM_NAME, new WSExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return
            "<subsystem xmlns=\"urn:jboss:domain:webservices:1.1\">" + 
            "    <modify-wsdl-address>true</modify-wsdl-address>" + 
            "    <wsdl-host>${jboss.bind.address:127.0.0.1}</wsdl-host>" + 
            "    <wsdl-port>8080</wsdl-port>" + 
            "    <wsdl-secure-port>8443</wsdl-secure-port>" + 
            "    <endpoint-config name=\"Standard-Endpoint-Config\"/>" + 
            "    <endpoint-config name=\"Recording-Endpoint-Config\">" + 
            "        <pre-handler-chain name=\"recording-handlers\" protocol-bindings=\"##SOAP11_HTTP ##SOAP11_HTTP_MTOM ##SOAP12_HTTP ##SOAP12_HTTP_MTOM\">" +
            "            <handler name=\"RecordingHandler\" class=\"org.jboss.ws.common.invocation.RecordingServerHandler\"/>" + 
            "        </pre-handler-chain>" +
            "        <property name=\"foo\" value=\"bar\"/>" + 
            "    </endpoint-config>" +
            "</subsystem>";
    }

}
