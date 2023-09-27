/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.pojos;

/**
 * @author <a href="mailto:opalka.richard@gmail.com">Richard Opalka</a>
 */
public interface POJOServiceMBean {

    boolean getStartCalled();

    boolean getStopCalled();

    boolean getCreateCalled();

    boolean getDestroyCalled();

    int getCount();

    int getInjectedCount();

}
