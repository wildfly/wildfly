/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.dispatcher.bean.legacy;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.group.Node;

/**
 * @author Paul Ferraro
 */
public class ExceptionCommand implements Command<Void, Node> {
    private static final long serialVersionUID = 8799161775551957641L;

    @Override
    public Void execute(Node context) throws Exception {
        throw new Exception();
    }
}
