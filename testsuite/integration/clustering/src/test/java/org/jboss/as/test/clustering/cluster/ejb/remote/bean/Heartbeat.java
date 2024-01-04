/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import java.util.Date;

public interface Heartbeat {
    Result<Date> pulse();
}
