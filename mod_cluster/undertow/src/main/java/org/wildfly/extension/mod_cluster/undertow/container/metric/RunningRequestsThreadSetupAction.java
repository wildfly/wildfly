/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.mod_cluster.undertow.container.metric;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ThreadSetupAction;
import org.wildfly.extension.mod_cluster.undertow.container.metric.jdk8backported.LongAdder;

/**
 * {@link ThreadSetupAction} implementation that counts number of active / running requests to replace the busyness
 * metric.
 *
 * @author Radoslav Husar
 * @version Aug 2013
 * @since 8.0
 */
public class RunningRequestsThreadSetupAction implements ThreadSetupAction {

    private static final LongAdder runningCount = new LongAdder();

    @Override
    public Handle setup(final HttpServerExchange exchange) {
        runningCount.increment();

        return new Handle() {
            @Override
            public void tearDown() {
                runningCount.decrement();
            }
        };
    }

    public static int getRunningRequestCount() {
        return runningCount.intValue();
    }
}
