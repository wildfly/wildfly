/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.session;

/**
 * @author baranowb
 */
public interface SessionConstants {
    String DEPLOYMENT_NAME_SESSION = "session";
    String DEPLOYMENT_JAR_NAME_SESSION = DEPLOYMENT_NAME_SESSION + ".jar";
    String TEST_MODULE_NAME = "Session";
    String TEST_MODULE_NAME_FULL = "test." + TEST_MODULE_NAME;

    String EJB_STATEFUL = "java:global/session/StatefulBeanWhichDependsOn!org.jboss.as.test.integration.ejb.singleton.dependson.session.Trigger";
    String EJB_STATELES = "java:global/session/StatelesBeanWhichDependsOn!org.jboss.as.test.integration.ejb.singleton.dependson.session.Trigger";

}
