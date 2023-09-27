/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

/**
 * @author baranowb
 */
public interface Constants {

    String TEST_MODULE_NAME = "MDBDepends";
    String TEST_MODULE_NAME_FULL = "test." + TEST_MODULE_NAME;
    String EJB_JMS_NAME = "java:module/JMSMessagingUtil";
    String QUEUE_NAME = "mdbtest/queue";
    String QUEUE_JNDI_NAME = "java:jboss/" + QUEUE_NAME;
    String QUEUE_REPLY_NAME = "mdbtest/replyQueue";
    String QUEUE_REPLY_JNDI_NAME = "java:jboss/" + QUEUE_REPLY_NAME;
    String SYS_PROP_KEY = "jboss/" + QUEUE_NAME;
    String SYS_PROP_VALUE = "activemq-ra.rar";

    String DEPLOYMENT_NAME_COUNTER = "callcounter";
    String DEPLOYMENT_JAR_NAME_COUNTER = DEPLOYMENT_NAME_COUNTER + ".jar";

    String DEPLOYMENT_NAME_MDB = "mdb";
    String DEPLOYMENT_JAR_NAME_MDB = DEPLOYMENT_NAME_MDB + ".jar";
    //dont use classes directly. Some are not shared.
    String SINGLETON_EJB = "java:global/callcounter/CallCounterSingleton!org.jboss.as.test.integration.ejb.singleton.dependson.mdb.CallCounterInterface";
}
