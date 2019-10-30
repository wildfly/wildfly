/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jaxrs.jackson;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author Stuart Douglas
 */
@Path("/jackson")
public class JacksonResource {

    @GET
    @Produces("application/vnd.customer+json")
    public Customer get() {
        return new Customer("John", "Citizen");
    }


    @Path("/duration")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Duration duration() {
        return Duration.of(1, ChronoUnit.SECONDS);
    }


    @Path("/optional")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Optional<String> optional() {
        return Optional.of("optional string");
    }
}
