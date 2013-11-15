/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.naming.shared;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
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
