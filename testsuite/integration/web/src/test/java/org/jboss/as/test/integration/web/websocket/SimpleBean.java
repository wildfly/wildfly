package org.jboss.as.test.integration.web.websocket;

import jakarta.enterprise.inject.Model;

@Model //to make it a bean without beans xml https://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#bean_defining_annotations
public class SimpleBean {

    public String sayHello() {
        return "No I wont";
    }
}
