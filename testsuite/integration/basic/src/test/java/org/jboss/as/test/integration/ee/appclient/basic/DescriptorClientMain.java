/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.appclient.basic;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
public class DescriptorClientMain {

    private static final Logger logger = Logger.getLogger("org.jboss.as.test.appclient");

    private static AppClientSingletonRemote appClientSingletonRemote;

    private static String envEntry;

    public static void main(final String[] params) {
        appClientSingletonRemote.makeAppClientCall(envEntry);
    }

}
