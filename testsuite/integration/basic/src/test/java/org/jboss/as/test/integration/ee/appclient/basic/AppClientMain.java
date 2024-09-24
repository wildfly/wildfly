/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.appclient.basic;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
public class AppClientMain {
    private static final Logger logger = Logger.getLogger("org.jboss.as.test.appclient");

    @Resource(lookup = "java:comp/InAppClientContainer")
    private static boolean appclient;

    @EJB
    private static AppClientSingletonRemote appClientSingletonRemote;

    public static void main(final String[] params) {
        logger.trace("Main method invoked");
        Status status = new Status();
        logger.info("Status class accessed via classloader = " + status.getClass().getClassLoader());
        if(!appclient) {
            logger.error("InAppClientContainer was not true");
            throw new RuntimeException("InAppClientContainer was not true");
        }

        try {
            appClientSingletonRemote.makeAppClientCall(params[0]);
            logger.trace("Main method invocation completed with success");
        } catch (Exception e) {
            logger.error("Main method failed", e);
        }
    }

}
