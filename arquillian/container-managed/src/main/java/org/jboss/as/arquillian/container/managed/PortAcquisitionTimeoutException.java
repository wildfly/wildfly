/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.managed;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;

/**
 * Denotes that a port could not be obtained within a designated timeout period.
 *
 * @author <a href="mailto:alr@jboss.org">ALR</a>
 * @see https://issues.jboss.org/browse/AS7-4070
 */
public class PortAcquisitionTimeoutException extends LifecycleException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new instance noting the port that could not be acquired in the designated amount of time
     *
     * @param ports
     * @param timeoutSeconds
     */
    public PortAcquisitionTimeoutException(final int port, final int timeoutSeconds) {
        super("Could not acquire requested port " + port + " in " + timeoutSeconds + " seconds");
    }
}
