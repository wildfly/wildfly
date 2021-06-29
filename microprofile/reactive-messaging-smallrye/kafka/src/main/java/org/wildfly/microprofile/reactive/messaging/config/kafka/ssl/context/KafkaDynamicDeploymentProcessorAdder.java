/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.microprofile.reactive.messaging.common.DynamicDeploymentProcessorAdder;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class KafkaDynamicDeploymentProcessorAdder implements DynamicDeploymentProcessorAdder {
    @Override
    public void addDeploymentProcessor(DeploymentProcessorTarget target, String subsystemName) {

        final int POST_MODULE_MICROPROFILE_REACTIVE_MESSAGING = 0x3828;

        target.addDeploymentProcessor(subsystemName,
                Phase.POST_MODULE,
                POST_MODULE_MICROPROFILE_REACTIVE_MESSAGING,
                new ReactiveMessagingSslConfigProcessor());
    }
}
