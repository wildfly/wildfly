/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.ejb.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

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
