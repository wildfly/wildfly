/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.dispatcher.bean;

import org.wildfly.clustering.server.dispatcher.Command;

import java.io.Serializable;

import org.wildfly.clustering.server.GroupMember;

/**
 * @author Paul Ferraro
 */
public class ExceptionCommand implements Command<Void, GroupMember, Exception>, Serializable {
    private static final long serialVersionUID = 8799161775551957641L;

    @Override
    public Void execute(GroupMember context) throws Exception {
        throw new Exception();
    }
}
