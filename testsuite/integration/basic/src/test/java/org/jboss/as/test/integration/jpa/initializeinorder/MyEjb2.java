package org.jboss.as.test.integration.jpa.initializeinorder;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author Stuart Douglas
 */
@Singleton
@Startup
public class MyEjb2 {

    @PostConstruct
    public void postConstruct() {
        InitializeInOrderTestCase.recordInit(MyEjb2.class.getSimpleName());
        System.out.println("xxx MyEjb2 postConstruct called, InitializeInOrderTestCase initOrder=" + InitializeInOrderTestCase.initOrder.toString());
    }
}
