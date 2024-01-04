/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Command to stop a singleton service.
 * @author Paul Ferraro
 */
public class StopCommand implements Command<Void, Lifecycle> {
    private static final long serialVersionUID = 3194143912789013071L;

    @Override
    public Void execute(Lifecycle context) throws Exception {
        context.stop();
        return null;
    }
}
