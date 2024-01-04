/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall.scoped.context;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Remote (StatelessRemoteOnOtherServer.class)
public class StatelessBeanOnOtherServer implements StatelessRemoteOnOtherServer {
    @Override
    public String echo(String msg) {
        return msg;
    }
}
