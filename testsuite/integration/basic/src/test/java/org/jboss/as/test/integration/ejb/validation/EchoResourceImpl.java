/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@Stateless
public class EchoResourceImpl implements EchoResource {
    private static final Logger log = LoggerFactory.getLogger(EchoResourceImpl.class);

    @Inject
    private DummyFlag dummyFlag;

    @Override
    public Response validateEchoThroughAbstractClass(DummySubclass payload) {
        dummyFlag.setExecutedServiceCallFlag(true);
        return Response.ok(payload.getDirection()).build();
    }

    @Override
    public Response validateEchoThroughClass(DummyClass payload) {
        dummyFlag.setExecutedServiceCallFlag(true);
        return Response.ok(payload.getDirection()).build();
    }
}
