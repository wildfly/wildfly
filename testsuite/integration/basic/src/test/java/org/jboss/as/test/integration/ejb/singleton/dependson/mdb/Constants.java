/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
