package org.jboss.as.test.integration.ejb.annotationprocessing;

import javax.ejb.EJB;

public interface ReferencingBeanBInterface {

    @EJB
    ReferencedBeanInterface refencedBean = null;

}
