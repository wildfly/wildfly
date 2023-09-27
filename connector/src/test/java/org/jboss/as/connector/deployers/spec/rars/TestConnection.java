/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import org.jboss.logging.Logger;

/**
 * TestConnection
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
public class TestConnection implements TestConnectionInterface {
    private static Logger log = Logger.getLogger(TestConnection.class);

    /**
     * CallMe
     **/
    public void callMe() {
        log.debug("call callMe");

    }

}
