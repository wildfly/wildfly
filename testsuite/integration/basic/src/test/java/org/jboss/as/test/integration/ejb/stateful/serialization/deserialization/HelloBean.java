/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.serialization.deserialization;

import org.jboss.logging.Logger;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import java.util.Calendar;

@Stateless
@Remote(HelloRemote.class)
public class HelloBean implements HelloRemote {
    private Logger log = Logger.getLogger(this.getClass().getSimpleName());

    public Response hello(LEContact request) {
        String startProcessing = Calendar.getInstance().getTime().toString();
        log.trace("------------ EJB Server Side processing message ------------");
        log.trace("------ Received Data Mutable_One: " + request.is_mutable_one());
        log.trace("------ Received Data Mutable_Two: " + request.is_mutable_two());
        log.trace("------ Received Data Mutable_Three: " + request.is_mutable_three());

        Response response = new Response();
        response.setStartProcessing(startProcessing);
        response.setRequestData(request.toString());
        response.setReturnData("Hello Simple EJB: " + request.toString());

        String finishProcessing = Calendar.getInstance().getTime().toString();
        response.setFinishProcessing(finishProcessing);

        return response;
    }
}