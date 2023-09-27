/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.sar;

import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ConfigService implements ConfigServiceMBean {

    Logger log = Logger.getLogger(ConfigService.class);

    private final String exampleName;

    private int interval;

    public ConfigService(String exampleName) {
        this.exampleName = exampleName;
    }

    public int getIntervalSeconds() {
        return interval;
    }

    public void setIntervalSeconds(int interval) {
        log.trace("Setting IntervalSeconds to " + interval);
        this.interval = interval;
    }

    public String getExampleName() {
        return exampleName;
    }
}
