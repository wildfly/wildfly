package org.jboss.as.test.integration.jpa.initializeinorder;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author Scott Marlow
 */
@Singleton
@Startup
public class MyEjb {

    @PostConstruct
    public void postConstruct() {
        InitializeInOrderTestCase.recordInit(MyEjb.class.getSimpleName());
        System.out.println("xxx MyEjb postConstruct called, InitializeInOrderTestCase initOrder=" + InitializeInOrderTestCase.initOrder.toString());
    }
}
