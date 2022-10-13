/*
 * Copyright 2022 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.manualmode.messaging;

import org.apache.activemq.artemis.api.core.client.loadbalance.ConnectionLoadBalancingPolicy;

/**
 * Simple add-on for Activemq Artemis.
 * @author Emmanuel Hugonnet (c) 2022 Red Hat, Inc.
 */
public class OrderedLoadBalancingPolicy implements ConnectionLoadBalancingPolicy {

    private int pos = -1;

    @Override
    public int select(final int max) {
        pos++;
        if (pos >= max) {
            pos = 0;
        }
        return pos;
    }
}
