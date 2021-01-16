/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.metrics.application.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Timed;

/**
 * REST end-point for Metered and Timed annotation testing, used in MicroProfileMetricsDifferentFormatsValueTestCase
 */
@Path("/")
public class ResourceMeteredTimed {
    /**
     * Test metered end-point
     *
     * @return 204 Response
     */
    @Metered(name = "metered",
            unit = MetricUnits.MINUTES,
            description = "Metrics to monitor metered method - @Metered.",
            absolute = true)
    @GET
    @Path("metered")
    public Response metered() {
        return Response.ok().build();
    }

    /**
     * Test timed end-point
     *
     * @return 204 Response
     */
    @Timed(name = "timed",
            description = "Metrics to monitor the times of timed method. - @Timed",
            unit = MetricUnits.MINUTES,
            absolute = true)
    @GET
    @Path("/timed")
    public Response timed() {
        return Response.ok().build();
    }
}
