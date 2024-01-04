/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.rts.jaxrs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.narayana.rest.integration.HeuristicMapper;
import org.jboss.narayana.rest.integration.ParticipantResource;

import jakarta.ws.rs.core.Application;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
public final class ParticipantApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<>(Arrays.asList(ParticipantResource.class, HeuristicMapper.class));
    }

}
