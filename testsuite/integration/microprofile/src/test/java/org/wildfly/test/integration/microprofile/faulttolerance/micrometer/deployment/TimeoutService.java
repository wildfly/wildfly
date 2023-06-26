/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.microprofile.faulttolerance.micrometer.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * A service that always times out!
 *
 * @author Radoslav Husar
 */
@Path("/timeout")
@ApplicationScoped
public class TimeoutService {

    @Fallback(fallbackMethod = "fallback")
    @Timeout(100)
    @GET
    public String alwaysTimeout() {
        try {
            // Note that sleep duration is longer than the timeout above.
            // Also note that this needs to be more than slightly higher than the timeout on slow/overloaded systems.
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            // Ignore.
        }
        return "standard method processing";
    }

    public String fallback() {
        return "fallback method called";
    }
}
