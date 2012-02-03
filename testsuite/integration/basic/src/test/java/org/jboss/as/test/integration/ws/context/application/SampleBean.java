package org.jboss.as.test.integration.ws.context.application;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jws.WebMethod;

@Stateless
@LocalBean
public class SampleBean {
    @WebMethod
    public String sayHello(String name) {
        return "Hello " + name + ".";
    }
}
