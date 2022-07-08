/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.undertow.sso.elytron;

import java.net.URI;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ee.BatchContext;
import org.wildfly.clustering.ee.Batcher;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.http.util.sso.SingleSignOn;

/**
 * @author Paul Ferraro
 */
public class DistributableSingleSignOn implements SingleSignOn {

    private final SSO<ElytronAuthentication, String, Map.Entry<String, URI>, LocalSSOContext> sso;
    private final Batcher<Batch> batcher;
    private final Batch batch;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DistributableSingleSignOn(
        final SSO< ElytronAuthentication, String, Map.Entry< String, URI >, LocalSSOContext > sso,
        final Batcher< Batch > batcher, final Batch batch ) {
        this.sso = sso;
        this.batcher = batcher;
        this.batch = batch;
    }

    @Override
    public String getId() {
        return this.sso.getId();
    }

    @Override
    public String getMechanism() {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            return this.sso.getAuthentication().getMechanism();
        }
    }

    @Override
    public boolean isProgrammatic() {
        return false;
    }

    @Override
    public String getName() {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            return this.sso.getAuthentication().getName();
        }
    }

    @Override
    public SecurityIdentity getIdentity() {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            return this.sso.getLocalContext().getSecurityIdentity();
        }
    }

    @Override
    public Map<String, Map.Entry<String, URI>> getParticipants() {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            final Sessions< String, Map.Entry< String, URI > > sessions = this.sso.getSessions();
            final Map< String, Map.Entry< String, URI > > participants = new HashMap<>();
            for( final String deployment : sessions.getDeployments() )
            {
                participants.put(deployment, sessions.getSession(deployment));
            }
            return Collections.unmodifiableMap(participants);
        }
    }

    @Override
    public void setIdentity( final SecurityIdentity identity ) {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            this.sso.getLocalContext().setSecurityIdentity(identity);
        }
    }

    @Override
    public boolean addParticipant( final String applicationId, final String sessionId, final URI participant ) {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            return this.sso.getSessions().addSession(applicationId, new AbstractMap.SimpleImmutableEntry<>(sessionId, participant));
        }
    }

    @Override
    public Map.Entry< String, URI > removeParticipant( final String applicationId ) {
        try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
        {
            return this.sso.getSessions().removeSession(applicationId);
        }
    }

    @Override
    public void invalidate() {
        // The batch associated with this SSO might not be valid (e.g. in the case of logout).
        try (final BatchContext context =
            this.closed.compareAndSet( false, true ) ? this.batcher.resumeBatch( this.batch ) : null)
        {
            try (final Batch batch = (context != null) ? this.batch : this.batcher.createBatch())
            {
                this.sso.invalidate();
            }
        }
    }

    @Override
    public void close() {
        if (this.closed.compareAndSet(false, true)) {
            try (final BatchContext context = this.batcher.resumeBatch( this.batch ))
            {
                this.batch.close();
            }
        }
    }
}
