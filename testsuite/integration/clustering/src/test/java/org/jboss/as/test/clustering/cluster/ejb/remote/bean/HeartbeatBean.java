/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote.bean;

import java.util.Date;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

@Stateless
@Remote(Heartbeat.class)
public class HeartbeatBean implements Heartbeat {

    @Override
    public Result<Date> pulse() {
        Date now = new Date();
        return new Result<>(now);
    }
}
