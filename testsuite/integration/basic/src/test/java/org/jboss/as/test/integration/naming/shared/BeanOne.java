/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.shared;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import javax.naming.InitialContext;

/**
 * @author Eduardo Martins
 */
@Startup
@Singleton
public class BeanOne {

    @EJB(lookup = "java:global/TEST_RESULTS_BEAN_JAR_NAME/TestResultsBean!org.jboss.as.test.integration.naming.shared.TestResults")
    private TestResults testResults;

    @PostConstruct
    public void postConstruct() {
        try {
            new InitialContext().lookup(TestResults.SHARED_BINDING_NAME_ONE);
            new InitialContext().rebind(TestResults.SHARED_BINDING_NAME_TWO,"");
            testResults.setPostContructOne(true);
        } catch (Throwable e) {
            e.printStackTrace();
            testResults.setPostContructOne(false);
        }
    }

    @PreDestroy
    public void preDestroy() {
        try {
            new InitialContext().lookup(TestResults.SHARED_BINDING_NAME_ONE);
            new InitialContext().lookup(TestResults.SHARED_BINDING_NAME_TWO);
            testResults.setPreDestroyOne(true);
        } catch (Throwable e) {
            e.printStackTrace();
            testResults.setPreDestroyOne(false);
        }
    }

}
