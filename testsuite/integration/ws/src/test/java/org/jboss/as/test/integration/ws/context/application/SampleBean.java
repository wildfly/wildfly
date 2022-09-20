package org.jboss.as.test.integration.ws.context.application;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.jws.WebMethod;

@Stateless
@LocalBean
public class SampleBean {
    @WebMethod
    public String sayHello(String name) {
        return "Hello " + name + ".";
    }
}
