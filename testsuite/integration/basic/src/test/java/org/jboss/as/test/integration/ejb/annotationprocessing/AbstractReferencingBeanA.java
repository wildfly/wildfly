package org.jboss.as.test.integration.ejb.annotationprocessing;

import javax.ejb.EJB;

public abstract class AbstractReferencingBeanA {

    @EJB
    private ReferencedBeanInterface referencedBean;

    public String relayHello() {
        return referencedBean.sayHello();
    }
}
