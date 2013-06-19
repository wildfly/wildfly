/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.extension.rts.common;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONArray;
import org.jboss.narayana.rest.integration.ParticipantInformation;
import org.jboss.narayana.rest.integration.ParticipantsContainer;
import org.jboss.narayana.rest.integration.api.Aborted;
import org.jboss.narayana.rest.integration.api.ParticipantsManagerFactory;
import org.jboss.narayana.rest.integration.api.Prepared;
import org.jboss.narayana.rest.integration.api.ReadOnly;
import org.jboss.narayana.rest.integration.api.Vote;

import java.lang.String;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
@Path("/" + TransactionalService.PATH_SEGMENT)
public final class TransactionalService {

    public static final String APPLICATION_ID = "org.wildfly.test.extension.rts";

    public static final String PATH_SEGMENT = "transactional-service";

    public static final String BASE_URL_PATH_SEGMENT = "base-url";

    @GET
    public Response getParticipantInvocations(@QueryParam("participantId") final String participantId) {
        ParticipantInformation information = ParticipantsContainer.getInstance().getParticipantInformation(participantId);

        if (information == null) {
            return Response.status(404).build();
        }

        final JSONArray jsonArray = new JSONArray(((LoggingParticipant)information.getParticipant()).getInvocations());

        return Response.ok().entity(jsonArray.toString()).build();
    }

    @POST
    public String enlistParticipant(@QueryParam("participantEnlistmentUrl") final String participantEnlistmentUrl,
            @QueryParam("vote") final String stringVote, @Context final UriInfo uriInfo) {

        final Vote vote = stringVoteToVote(stringVote);
        final LoggingParticipant participant = new LoggingParticipant(vote);
        ParticipantsManagerFactory.getInstance().setBaseUrl(uriInfo.getBaseUri().toString());

        return ParticipantsManagerFactory.getInstance().enlist(APPLICATION_ID, participantEnlistmentUrl, participant).toString();
    }

    @PUT
    @Path("/" + TransactionalService.BASE_URL_PATH_SEGMENT)
    public void setBaseUrl(@Context final UriInfo uriInfo) {
        ParticipantsManagerFactory.getInstance().setBaseUrl(uriInfo.getBaseUri().toString());
    }

    private Vote stringVoteToVote(final String stringVote) {
        if (Prepared.class.getName().equals(stringVote)) {
            return new Prepared();
        } else if (ReadOnly.class.getName().equals(stringVote)) {
            return new ReadOnly();
        }

        return new Aborted();
    }

}
