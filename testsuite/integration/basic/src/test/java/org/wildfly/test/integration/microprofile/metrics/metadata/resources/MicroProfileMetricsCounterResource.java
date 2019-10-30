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

package org.wildfly.test.integration.microprofile.metrics.metadata.resources;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/counter")
public class MicroProfileMetricsCounterResource {

   @Inject
   MetricRegistry registry;

   @GET
   @Path("/hello")
   public Response hello() {
      Metadata counterMetadata = new Metadata("helloCounter", MetricType.COUNTER);

      // TODO: Remove following line once https://github.com/smallrye/smallrye-metrics/issues/43 is fixed
      counterMetadata.setReusable(true); // workaround

      registry.counter(counterMetadata).inc();
      return Response.ok("Hello World!").build();
   }
}
