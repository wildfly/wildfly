package org.jboss.as.test.integration.ee.initializeinorder;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author Stuart Douglas
 */
@Singleton
@Startup
public class MyEjb {

    @PostConstruct
    public void postConstruct() {
        InitializeInOrderTestCase.recordInit(MyEjb.class.getSimpleName());
    }
}
