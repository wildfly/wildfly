/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import org.jboss.logging.Logger;

/**
 * @author John E. Bailey
 */
public class LegacyService implements LegacyServiceMBean {

    private static final Logger logger = Logger.getLogger(LegacyService.class);

    private LegacyService other;
    private String somethingElse;

    public LegacyService() {
    }

    public LegacyService(String somethingElse) {
        this.somethingElse = somethingElse;
    }

    public void setOther(LegacyService other) {
        this.other = other;
    }

    public LegacyService getOther() {
        return other;
    }

    public String getSomethingElse() {
        return somethingElse;
    }

    public String appendSomethingElse(String more) {
        return somethingElse + " - " + more;
    }

    public void setSomethingElse(String somethingElse) {
        this.somethingElse = somethingElse;
    }

    public void start() {
        logger.info("Started");
    }

    public void stop() {
        logger.info("Stopped");
    }
}
