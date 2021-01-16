package org.jboss.as.test.integration.ws.basic;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.BindingType;


@WebService(
        serviceName = "SimpleService",
        targetNamespace = "http://jbossws.org/basic",
        endpointInterface = "org.jboss.as.test.integration.ws.basic.InstanceCountEndpointIface"
)
@BindingType(javax.xml.ws.soap.SOAPBinding.SOAP12HTTP_BINDING)
public class InstanceCountEndpoint implements InstanceCountEndpointIface {

    public static int instanceNum = 0;
    private Log log = LogFactory.getLog(InstanceCountEndpoint.class);

    public InstanceCountEndpoint() {
        instanceNum++;
    }

    @WebMethod
    public int getInstanceCount() {
        return instanceNum;
    }

    @WebMethod
    public String test(String payload) {
        log.debug("Start test(). payload=" + payload);
        return "OK";
    }
}
