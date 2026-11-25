/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import java.util.Date;

import jakarta.ejb.EJBObject;

/**
 * The remote / business interface for the Heartbeat Enterprise Beans 2.x bean
 *
 * @author Richard Achmatowicz
 */
public interface HeartbeatRemote extends EJBObject {
    Result<Date> pulse();
}
