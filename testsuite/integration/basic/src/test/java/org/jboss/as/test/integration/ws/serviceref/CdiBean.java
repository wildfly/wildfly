package org.jboss.as.test.integration.ws.serviceref;

import javax.xml.ws.WebServiceRef;

/**
 * @author Stuart Douglas
 */
public class CdiBean {

    @WebServiceRef(value = EndpointService.class, mappedName = "jbossws-client/service/TestService", wsdlLocation = "META-INF/wsdl/TestService.wsdl")
    EndpointInterface endpoint1;


    public String echo(String input) {
        return endpoint1.echo(input);
    }
}
