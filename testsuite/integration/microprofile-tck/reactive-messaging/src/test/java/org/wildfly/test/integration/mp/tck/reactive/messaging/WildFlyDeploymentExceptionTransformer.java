/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.mp.tck.reactive.messaging;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyDeploymentExceptionTransformer implements DeploymentExceptionTransformer {
    @Override
    public Throwable transform(Throwable throwable) {
        if (throwable instanceof org.jboss.arquillian.container.spi.client.container.DeploymentException) {
            // This ends up running on the client, and the arq DeploymentException does not actually
            // have the cause populated. However, the message contains a summary of what happened on the server,
            // and will look something like:
            //
            // Cannot deploy test.war: {"WFLYCTL0062: Composite operation failed and was rolled back. Steps that failed:" => {"Operation step-1" => {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"test.war\".WeldStartService" => "Failed to start service
            //     Caused by: org.jboss.weld.exceptions.DeploymentException: Unknown connector for dummy-source-2.
            //     Caused by: java.lang.IllegalArgumentException: Unknown connector for dummy-source-2."}}}}
            //
            // So we parse the string looking for the relevant parts here. The Weld DeploymentException extends
            // the javax.enterprise.inject.spi.DeploymentException wanted by the test, so it has happened if
            // we can find it in the exception message.
            String msg = throwable.getMessage();
            if (msg.contains("WFLYCTL0080") && msg.contains("org.jboss.weld.exceptions.DeploymentException")) {
                return new DeploymentException(msg);
            }
        }
        return null;
    }
}
