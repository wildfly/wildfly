package org.jboss.as.test.integration.ejb.annotationprocessing;

import javax.ejb.Stateless;

@Stateless
public class ReferencedBean implements ReferencedBeanInterface {
    @Override
    public String sayHello() {
        return "Hello!";
    }
}
