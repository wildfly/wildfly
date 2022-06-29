package org.jboss.as.test.integration.ee.initializeinorder;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

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
