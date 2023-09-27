/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.sar;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface ConfigServiceMBean {
    int getIntervalSeconds();

    void setIntervalSeconds(int interval);

    String getExampleName();
}
