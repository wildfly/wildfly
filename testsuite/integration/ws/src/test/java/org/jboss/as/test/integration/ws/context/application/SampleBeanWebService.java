package org.jboss.as.test.integration.ws.context.application;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jws.WebMethod;
import javax.jws.WebService;

@Stateless
@LocalBean
@WebService
public class SampleBeanWebService {
    @WebMethod
    public String sayHello(String name) {
        return "Hello " + name + ".";
    }
}
