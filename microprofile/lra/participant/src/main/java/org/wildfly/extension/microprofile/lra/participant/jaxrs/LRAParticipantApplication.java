/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.microprofile.lra.participant.jaxrs;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationPath("/")
public class LRAParticipantApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(List.of(LRAParticipantResource.class));
    }

}
