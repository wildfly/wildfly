/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

package org.jboss.as.clustering.web.sso.infinispan;

import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.as.clustering.infinispan.atomic.AtomicMapCache;
import org.jboss.as.clustering.infinispan.invoker.BatchOperation;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.web.sso.FullyQualifiedSessionId;
import org.jboss.as.clustering.web.sso.SSOCredentials;
import org.jboss.as.clustering.web.sso.SSOLocalManager;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * An implementation of SSOClusterManager that uses a Infinispan cache to share SSO information between cluster nodes.
 *
 * @author Brian E. Stansberry
 * @author Scott Marlow
 * @version $Revision: 109002 $ $Date: 2007-01-12 03:39:24 +0100 (ven., 12 janv. 2007) $
 */
@Listener
public final class SSOClusterManager implements org.jboss.as.clustering.web.sso.SSOClusterManager {

    private static final Logger log = Logger.getLogger(SSOClusterManager.class);;

    /**
     * The clustered cache that holds the SSO credentials and the sessions. The CacheKey will indicate which type it is
     * (CacheKey.CREDENTIAL or CacheKey.SESSION);
     */
    private volatile Cache<SSOKey, Object> cache;
    private volatile Cache<CredentialKey, SSOCredentials> credentialCache;
    private volatile Cache<SessionKey, Map<FullyQualifiedSessionId, Void>> sessionCache;
    @SuppressWarnings("rawtypes")
    private final InjectedValue<Cache> cacheRef = new InjectedValue<Cache>();
    private volatile String cacheContainerName = "web";
    private volatile String cacheName = "sso";

    /**
     * The SingleSignOn for which we are providing cluster support
     */
    private volatile SSOLocalManager ssoValve = null;

    @Override
    public void addDependencies(ServiceTarget target, ServiceBuilder<?> builder) {
        builder.addDependency(CacheService.getServiceName(this.cacheContainerName, this.cacheName), Cache.class, this.cacheRef);
    }

    @Override
    public void setCacheContainerName(String cacheContainerName) {
        this.cacheContainerName = cacheContainerName;
    }

    @Override
    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * Notify the cluster of the addition of a Session to an SSO session.
     *
     * @param ssoId the id of the SSO session
     * @param sessionId id of the Session that has been added
     */
    @Override
    public void addSession(String ssoId, final FullyQualifiedSessionId sessionId) {
        if (log.isTraceEnabled())
            log.tracef("addSession(): adding Session %s to cached session set for SSO %s", sessionId.getSessionId(), ssoId);

        final SessionKey key = new SessionKey(ssoId);
        SessionOperation<Void> operation = new SessionOperation<Void>() {
            @Override
            public Void invoke(Cache<SessionKey, Map<FullyQualifiedSessionId, Void>> cache) {
                cache.putIfAbsent(key, null).put(sessionId, null);
                return null;
            }
        };
        this.batch(this.sessionCache, operation);
    }

    /**
     * Gets the SingleSignOn valve for which this object is handling cluster communications.
     *
     * @return the <code>SingleSignOn</code> valve.
     */
    @Override
    public SSOLocalManager getSSOLocalManager() {
        return ssoValve;
    }

    /**
     * Sets the SingleSignOn valve for which this object is handling cluster communications.
     * <p>
     * <b>NOTE:</b> This method must be called before calls can be made to the other methods of this interface.
     *
     * @param localManager a <code>SingleSignOn</code> valve.
     */
    @Override
    public void setSSOLocalManager(SSOLocalManager localManager) {
        ssoValve = localManager;
    }

    /**
     * Notifies the cluster that a single sign on session has been terminated due to a user logout.
     *
     * @param ssoId
     */
    @Override
    public void logout(final String ssoId) {
        if (log.isTraceEnabled())
            log.tracef("Registering logout of SSO %s in clustered cache", ssoId);

        Operation<Void> operation = new Operation<Void>() {
            @Override
            public Void invoke(Cache<SSOKey, Object> cache) {
                Cache<SSOKey, Object> removeCache = cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP);
                removeCache.remove(new SessionKey(ssoId));
                removeCache.remove(new CredentialKey(ssoId));
                return null;
            }
        };
        this.batch(this.cache, operation);
    }

    @Override
    public SSOCredentials lookup(String ssoId) {
        return this.credentialCache.get(new CredentialKey(ssoId));
    }

    /**
     * Notifies the cluster of the creation of a new SSO entry.
     *
     * @param ssoId the id of the SSO session
     * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST or FORM) used to authenticate the SSO.
     * @param username the username (if any) used for the authentication
     * @param password the password (if any) used for the authentication
     */
    @Override
    public void register(String ssoId, String authType, String username, String password) {
        if (log.isTraceEnabled())
            log.tracef("Registering SSO %s in clustered cache", ssoId);

        storeCredentials(ssoId, authType, username, password);
    }

    /**
     * Notify the cluster of the removal of a Session from an SSO session. May be called from ssoValue.deregister(String ssoId,
     * String session)
     *
     * @param ssoId the id of the SSO session
     * @param sessionId id of the Session that has been removed
     */
    @Override
    public void removeSession(String ssoId, final FullyQualifiedSessionId sessionId) {
        if (log.isTraceEnabled())
            log.tracef("removeSession(): removing Session %s from cached session set for SSO %s", sessionId.getSessionId(), ssoId);

        final SessionKey key = new SessionKey(ssoId);
        SessionOperation<Boolean> operation = new SessionOperation<Boolean>() {
            @Override
            public Boolean invoke(Cache<SessionKey, Map<FullyQualifiedSessionId, Void>> cache) {
                Map<FullyQualifiedSessionId, Void> sessions = cache.get(key);
                if (sessions == null) {
                    return false;
                }
                sessions.remove(sessionId);
                return sessions.isEmpty();
            }
        };
        if (this.batch(this.sessionCache, operation)) {
            this.notifySSOEmpty(ssoId);
        }
    }

    /**
     * Notifies the cluster of an update of the security credentials associated with an SSO session.
     *
     * @param ssoId the id of the SSO session
     * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST or FORM) used to authenticate the SSO.
     * @param username the username (if any) used for the authentication
     * @param password the password (if any) used for the authentication
     */
    @Override
    public void updateCredentials(String ssoId, String authType, String username, String password) {
        if (log.isTraceEnabled())
            log.tracef("Updating credentials for SSO %s in clustered cache", ssoId);

        storeCredentials(ssoId, authType, username, password);
    }

    // ------------------------------------------------------ CacheListener

    /**
     * Extracts an SSO session id and uses it in an invocation of {@link ClusteredSingleSignOn#deregister(String)
     * ClusteredSingleSignOn.deregister(String)}.
     * <p/>
     * Ignores invocations resulting from Cache changes originated by this object.
     *
     * @param event
     */
    @CacheEntryRemoved
    public void cacheEntryRemoved(CacheEntryRemovedEvent<SSOKey, ?> event) {
        if (log.isTraceEnabled()) {
            boolean isPre = event.isPre() ;
            boolean isOriginLocal = event.isOriginLocal();
            log.tracef("Received CacheEntryRemovedEvent from cluster: isPre = %s isOrigin = %s",
                    Boolean.toString(isPre), Boolean.toString(isOriginLocal));
        }

        if (event.isPre()) return;

        SSOKey key = event.getKey();
        String ssoId = key.getId();

        if (key instanceof SessionKey) {
            if (log.isTraceEnabled())
                log.tracef("cacheEntryRemoved ssoId = %s", ssoId);

            if (!event.isOriginLocal()) {
                if (log.isTraceEnabled())
                    log.trace("cacheEntryRemoved: event is not local- degeristering SSO key");

                this.ssoValve.deregisterLocal(key.getId());
            }
            // signal the case that we have zero sessions for this ssoId
            if (log.isTraceEnabled())
                log.trace("CacheEntryRemoved: notifying SSO empty");
            this.ssoValve.notifySSOEmpty(ssoId);
        }
    }

    /**
     * Checks whether any peers remain for the given SSO; if not notifies the valve that the SSO is empty.
     *
     * @param ssoId
     * @return true if SSO has zero sessions
     */
    private boolean notifySSOEmpty(String ssoId) {
        SessionKey key = new SessionKey(ssoId);
        boolean empty = this.sessionCache.containsKey(key) ? this.sessionCache.get(key).isEmpty() : true;
        if (empty) {
            ssoValve.notifySSOEmpty(ssoId);
        }
        return empty;
    }

    /**
     * Extracts an SSO session id and uses it in an invocation of {@link ClusteredSingleSignOn#update
     * ClusteredSingleSignOn.update()}.
     * <p/>
     * Ignores invocations resulting from Cache changes originated by this object.
     * <p/>
     * Ignores invocations for SSO session id's that are not registered with the local SingleSignOn valve.
     *
     * @param event
     */
    @CacheEntryModified
    public void cacheEntryModified(CacheEntryModifiedEvent<SSOKey, ?> event) {
        if (log.isTraceEnabled()) {
            boolean isPre = event.isPre() ;
            boolean isOriginLocal = event.isOriginLocal();
            log.tracef("Received CacheEntryModifiedEvent from cluster: isPre = %s isOrigin = %s",
                    Boolean.toString(isPre), Boolean.toString(isOriginLocal));
        }

        if (event.isPre() || event.isOriginLocal()) return;

        SSOKey key = event.getKey();
        String ssoId = key.getId();

        if (key instanceof CredentialKey) {
            if (log.isTraceEnabled())
                log.tracef("received a credentials modified message for SSO %s", ssoId);
            SSOCredentials credentials = (SSOCredentials) event.getValue();
            if (credentials != null) {
                this.ssoValve.remoteUpdate(ssoId, credentials);
            }
        } else if (key instanceof SessionKey) {
            if (log.isTraceEnabled())
                log.tracef("received a session modified message for SSO %s", ssoId);
            if (!this.notifySSOEmpty(ssoId)) {
                this.ssoValve.notifySSONotEmpty(ssoId);
            }
        }
    }

    /**
     * Prepare for the beginning of active use of the public methods of this component. This method should be called before any
     * of the public methods of this component are utilized. It should also send a LifecycleEvent of type START_EVENT to any
     * registered listeners.
     *
     * @throws Exception if this component detects a fatal error that prevents this component from being used
     */
    @SuppressWarnings("unchecked")
    @Override
    public void start() throws Exception {
        @SuppressWarnings("rawtypes")
        AdvancedCache cache = this.cacheRef.getValue().getAdvancedCache();
        this.cache = cache;
        this.credentialCache = cache;
        this.sessionCache = new AtomicMapCache<SessionKey, FullyQualifiedSessionId, Void>(cache);

        this.cache.addListener(this);
    }

    /**
     * Gracefully terminate the active use of the public methods of this component. This method should be the last one called on
     * a given instance of this component. It should also send a LifecycleEvent of type STOP_EVENT to any registered listeners.
     *
     * @throws Exception if this component detects a fatal error that needs to be reported
     */
    @Override
    public void stop() throws Exception {
        this.cache.removeListener(this);
    }

    /**
     * Stores the given data to the clustered cache.
     *
     * @param ssoId the id of the SSO session
     * @param authType the type of authenticator (BASIC, CLIENT-CERT, DIGEST or FORM) used to authenticate the SSO.
     * @param username the username (if any) used for the authentication
     * @param password the password (if any) used for the authentication
     */
    private void storeCredentials(final String ssoId, String authType, String username, String password) {
        final SSOCredentials credentials = new SSOCredentials(authType, username, password);

        CredentialOperation<Void> operation = new CredentialOperation<Void>() {
            @Override
            public Void invoke(Cache<CredentialKey, SSOCredentials> cache) {
                cache.getAdvancedCache().withFlags(Flag.SKIP_REMOTE_LOOKUP).put(new CredentialKey(ssoId), credentials);
                return null;
            }
        };
        this.batch(this.credentialCache, operation);
    }

    <K extends SSOKey, V, R> R batch(Cache<K, V> cache, CacheInvoker.Operation<K, V, R> operation) {
        return new BatchOperation<K, V, R>(operation).invoke(cache);
    }

    abstract class Operation<R> implements CacheInvoker.Operation<SSOKey, Object, R> {
    }
    abstract class CredentialOperation<R> implements CacheInvoker.Operation<CredentialKey, SSOCredentials, R> {
    }
    abstract class SessionOperation<R> implements CacheInvoker.Operation<SessionKey, Map<FullyQualifiedSessionId, Void>, R> {
    }

}
