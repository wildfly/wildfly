/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.SessionEvent;
import org.apache.catalina.SessionListener;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.session.StandardSessionFacade;
import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.StringManager;
import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.jboss.as.clustering.web.DistributedCacheManager;
import org.jboss.as.clustering.web.IncomingDistributableSessionData;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.SessionOwnershipSupport;
import org.jboss.as.web.session.notification.ClusteredSessionManagementStatus;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationCause;
import org.jboss.as.web.session.notification.ClusteredSessionNotificationPolicy;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.ReplicationTrigger;

/**
 * Abstract base class for session clustering based on StandardSession. Different session replication strategies can be
 * implemented by subclasses.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 *
 * @version $Revision: 109139 $
 */
public abstract class ClusteredSession<O extends OutgoingDistributableSessionData> implements HttpSession, Session {
    protected static final boolean ACTIVITY_CHECK = Globals.STRICT_SERVLET_COMPLIANCE
            || Boolean.valueOf(System.getProperty("org.apache.catalina.session.StandardSession.ACTIVITY_CHECK", "false")).booleanValue();

    /**
     * Descriptive information describing this Session implementation.
     */
    protected static final String info = "ClusteredSession/1.0";

    /**
     * Set of attribute names which are not allowed to be replicated/persisted.
     */
    protected static final String[] excludedAttributes = { Globals.SUBJECT_ATTR };

    /**
     * Set containing all members of {@link #excludedAttributes}.
     */
    protected static final Set<String> replicationExcludes = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(excludedAttributes)));

    /**
     * The method signature for the <code>fireContainerEvent</code> method.
     */
    protected static final Class<?>[] containerEventTypes = { String.class, Object.class };

    protected static final Logger log = Logger.getLogger(ClusteredSession.class);

    /**
     * The dummy HTTP session context used for servlet spec compliance.
     */
    @SuppressWarnings("deprecation")
    protected static javax.servlet.http.HttpSessionContext sessionContext = new javax.servlet.http.HttpSessionContext() {
        @Override
        public Enumeration<String> getIds() {
            return Collections.enumeration(Collections.<String> emptyList());
        }

        @Override
        public HttpSession getSession(String sessionId) {
            return null;
        }
    };

    /**
     * The string manager for this package.
     */
    protected static final StringManager sm = StringManager.getManager(ClusteredSession.class.getPackage().getName());

    /** Length of time we do full replication as workaround to JBCACHE-1531 */
    private static final long FULL_REPLICATION_WINDOW_LENGTH = 5000;

    // ----------------------------------------------------- Instance Variables

    /**
     * The collection of user data attributes associated with this Session.
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>(16, 0.75f, 2);

    /**
     * The authentication type used to authenticate our cached Principal, if any. NOTE: This value is not included in the
     * serialized version of this object.
     */
    private transient String authType = null;

    /**
     * The <code>java.lang.Method</code> for the <code>fireContainerEvent()</code> method of the
     * <code>org.apache.catalina.core.StandardContext</code> method, if our Context implementation is of this class. This value
     * is computed dynamically the first time it is needed, or after a session reload (since it is declared transient).
     */
    private transient Method containerEventMethod = null;

    /**
     * The time this session was created, in milliseconds since midnight, January 1, 1970 GMT.
     */
    private long creationTime = 0L;

    /**
     * We are currently processing a session expiration, so bypass certain IllegalStateException tests. NOTE: This value is not
     * included in the serialized version of this object.
     */
    private transient volatile boolean expiring = false;

    /**
     * The facade associated with this session. NOTE: This value is not included in the serialized version of this object.
     */
    private transient StandardSessionFacade facade = null;

    /**
     * The session identifier of this Session.
     */
    private String id = null;

    /**
     * The last accessed time for this Session.
     */
    private volatile long lastAccessedTime = creationTime;

    /**
     * The session event listeners for this Session.
     */
    private transient List<SessionListener> listeners = new ArrayList<SessionListener>();

    /**
     * The Manager with which this Session is associated.
     */
    private transient ClusteredSessionManager<O> manager = null;

    /**
     * Our proxy to the distributed cache.
     */
    private transient DistributedCacheManager<O> distributedCacheManager;

    /**
     * The maximum time interval, in seconds, between client requests before the servlet container may invalidate this session.
     * A negative time indicates that the session should never time out.
     */
    private int maxInactiveInterval = -1;

    /**
     * Flag indicating whether this session is new or not.
     */
    private boolean isNew = false;

    /**
     * Flag indicating whether this session is valid or not.
     */
    private volatile boolean isValid = false;

    /**
     * Internal notes associated with this session by Catalina components and event listeners. <b>IMPLEMENTATION NOTE:</b> This
     * object is <em>not</em> saved and restored across session serializations!
     */
    private final transient Map<String, Object> notes = new Hashtable<String, Object>();

    /**
     * The authenticated Principal associated with this session, if any. <b>IMPLEMENTATION NOTE:</b> This object is <i>not</i>
     * saved and restored across session serializations!
     */
    private transient Principal principal = null;

    /**
     * The property change support for this component. NOTE: This value is not included in the serialized version of this
     * object.
     */
    private transient PropertyChangeSupport support = new PropertyChangeSupport(this);

    /**
     * The current accessed time for this session.
     */
    private volatile long thisAccessedTime = creationTime;

    /**
     * The access count for this session.
     */
    private final transient AtomicInteger accessCount;

    /**
     * Policy controlling whether reading/writing attributes requires replication.
     */
    private ReplicationTrigger invalidationPolicy;

    /**
     * If true, means the local in-memory session data contains metadata changes that have not been published to the distributed
     * cache.
     */
    private transient boolean sessionMetadataDirty;

    /**
     * If true, means the local in-memory session data contains attribute changes that have not been published to the
     * distributed cache.
     */
    private transient boolean sessionAttributesDirty;

    /**
     * Object wrapping thisAccessedTime. Create once and mutate so we can store it in JBoss Cache w/o concern that a transaction
     * rollback will revert the cached ref to an older object.
     */
    private final transient AtomicLong timestamp = new AtomicLong(0);

    /**
     * Object wrapping other metadata for this session. Create once and mutate so we can store it in JBoss Cache w/o concern
     * that a transaction rollback will revert the cached ref to an older object.
     */
    private transient volatile DistributableSessionMetadata metadata = new DistributableSessionMetadata();

    /**
     * The last time {@link #setIsOutdated setIsOutdated(true)} was called or <code>0</code> if
     * <code>setIsOutdated(false)</code> was subsequently called.
     */
    private transient volatile long outdatedTime;

    /**
     * Version number to track cache invalidation. If any new version number is greater than this one, it means the data it
     * holds is newer than this one.
     */
    private final AtomicInteger version = new AtomicInteger(0);

    /**
     * The session's id with any jvmRoute removed.
     */
    private transient String realId;

    /**
     * Timestamp when we were last replicated.
     */
    private transient volatile long lastReplicated;

    /**
     * Maximum number of milliseconds this session should be allowed to go unreplicated if access to the session doesn't mark it
     * as dirty.
     */
    private transient long maxUnreplicatedInterval;

    /** True if maxUnreplicatedInterval is 0 or less than maxInactiveInterval */
    private transient boolean alwaysReplicateTimestamp = true;

    /**
     * Whether any of this session's attributes implement HttpSessionActivationListener.
     */
    private transient Boolean hasActivationListener;

    /**
     * Has this session only been accessed once?
     */
    private transient boolean firstAccess;

    /**
     * Policy that drives whether we issue servlet spec notifications.
     */
    private transient ClusteredSessionNotificationPolicy notificationPolicy;

    private transient ClusteredSessionManagementStatus clusterStatus;

    /** True if a call to activate() is needed to offset a preceding passivate() call */
    private transient boolean needsPostReplicateActivation;

    /**
     * True if a getOutgoingSessionData() should include metadata and all attributes no matter what. This is a workaround to
     * JBCACHE-1531. This flag ensures that at least one request gets full replication, whether or not in occurs before
     * this.fullReplicationWindow
     */
    private transient boolean fullReplicationRequired = true;
    /** End of period when we do full replication */
    private transient long fullReplicationWindow = -1;

    /** Coordinate updates from the cluster */
    private transient Lock ownershipLock = new ReentrantLock();

    // ------------------------------------------------------------ Constructors

    /**
     * Creates a new ClusteredSession.
     *
     * @param manager the manager for this session
     */
    protected ClusteredSession(ClusteredSessionManager<O> manager) {
        super();

        // Initialize access count
        accessCount = ACTIVITY_CHECK ? new AtomicInteger() : null;
        this.firstAccess = true;

        setManager(manager);
        requireFullReplication();
    }

    // ---------------------------------------------------------------- Session

    @Override
    public String getAuthType() {
        return this.authType;
    }

    @Override
    public void setAuthType(String authType) {
        String oldAuthType = this.authType;
        this.authType = authType;
        support.firePropertyChange("authType", oldAuthType, this.authType);
    }

    @Override
    public long getCreationTime() {
        if (!isValidInternal())
            throw new IllegalStateException(sm.getString("clusteredSession.getCreationTime.ise"));

        return (this.creationTime);
    }

    /**
     * Set the creation time for this session. This method is called by the Manager when an existing Session instance is reused.
     *
     * @param time The new creation time
     */
    @Override
    public void setCreationTime(long time) {
        this.creationTime = time;
        this.lastAccessedTime = time;
        this.thisAccessedTime = time;
        sessionMetadataDirty();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getIdInternal() {
        return this.id;
    }

    /**
     * Overrides the superclass method to also set the {@link #getRealId() realId} property.
     */
    @Override
    public void setId(String id) {
        // Parse the real id first, as super.setId() calls add(),
        // which depends on having the real id
        parseRealId(id);

        if ((this.id != null) && (manager != null)) {
            manager.remove(this);
        }
        this.id = id;

        this.clusterStatus = new ClusteredSessionManagementStatus(this.realId, true, null, null);

        if (manager != null) {
            manager.add(this);
        }
    }

    @Override
    public long getLastAccessedTime() {
        if (!isValidInternal()) {
            throw new IllegalStateException(sm.getString("clusteredSession.getLastAccessedTime.ise"));
        }

        return this.lastAccessedTime;
    }

    @Override
    public long getLastAccessedTimeInternal() {
        return (this.lastAccessedTime);
    }

    @Override
    public Manager getManager() {
        return (this.manager);
    }

    @Override
    public void setManager(Manager manager) {
        if ((manager instanceof ClusteredSessionManager<?>) == false)
            throw new IllegalArgumentException("manager must implement ClusteredManager");
        @SuppressWarnings("unchecked")
        ClusteredSessionManager<O> unchecked = (ClusteredSessionManager<O>) manager;
        this.manager = unchecked;

        this.invalidationPolicy = this.manager.getReplicationTrigger();

        int maxUnrep = this.manager.getMaxUnreplicatedInterval() * 1000;
        setMaxUnreplicatedInterval(maxUnrep);
        this.notificationPolicy = this.manager.getNotificationPolicy();
        establishDistributedCacheManager();
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    /**
     * Overrides the superclass to calculate {@link #getMaxUnreplicatedInterval() maxUnreplicatedInterval}.
     */
    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
        checkAlwaysReplicateTimestamp();
        sessionMetadataDirty();
    }

    @Override
    public Principal getPrincipal() {
        return this.principal;
    }

    /**
     * Set the authenticated Principal that is associated with this Session. This provides an <code>Authenticator</code> with a
     * means to cache a previously authenticated Principal, and avoid potentially expensive <code>Realm.authenticate()</code>
     * calls on every request.
     *
     * @param principal The new Principal, or <code>null</code> if none
     */
    @Override
    public void setPrincipal(Principal principal) {
        Principal oldPrincipal = this.principal;
        this.principal = principal;
        support.firePropertyChange("principal", oldPrincipal, this.principal);

        if ((oldPrincipal != null && !oldPrincipal.equals(principal)) || (oldPrincipal == null && principal != null)) {
            sessionMetadataDirty();
        }
    }

    @Override
    public void access() {
        this.acquireSessionOwnership();

        this.lastAccessedTime = this.thisAccessedTime;
        this.thisAccessedTime = System.currentTimeMillis();

        if (ACTIVITY_CHECK) {
            accessCount.incrementAndGet();
        }

        // JBAS-3528. If it's not the first access, make sure
        // the 'new' flag is correct
        if (!firstAccess && isNew) {
            setNew(false);
        }
    }

    private void acquireSessionOwnership() {
        SessionOwnershipSupport support = this.distributedCacheManager.getSessionOwnershipSupport();

        if (support != null) {
            try {
                this.ownershipLock.lockInterruptibly();

                try {
                    if (support.acquireSessionOwnership(this.realId, needNewLock()) == SessionOwnershipSupport.LockResult.ACQUIRED_FROM_CLUSTER) {
                        IncomingDistributableSessionData data = this.distributedCacheManager.getSessionData(this.realId, false);
                        if (data != null) {
                            // We may be out of date re: the distributed cache
                            update(data);
                        }
                    }
                } catch (TimeoutException e) {
                    throw new RuntimeException("Caught " + e.getClass().getSimpleName() + " acquiring ownership of " + this.realId, e);
                } finally {
                    this.ownershipLock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while acquiring ownership of " + this.realId, e);
            }
        }
    }

    private boolean needNewLock() {
        return firstAccess && isNew;
    }

    @Override
    public void endAccess() {
        isNew = false;

        if (ACTIVITY_CHECK) {
            accessCount.decrementAndGet();
        }

        this.lastAccessedTime = this.thisAccessedTime;

        if (firstAccess) {
            firstAccess = false;
            // Tomcat marks the session as non new, but that's not really
            // accurate per SRV.7.2, as the second request hasn't come in yet
            // So, we fix that
            isNew = true;
        }

        this.relinquishSessionOwnership(false);
    }

    private void relinquishSessionOwnership(boolean remove) {
        SessionOwnershipSupport support = this.distributedCacheManager.getSessionOwnershipSupport();

        if (support != null) {
            support.relinquishSessionOwnership(this.realId, remove);
        }
    }

    @Override
    public boolean isNew() {
        if (!isValidInternal())
            throw new IllegalStateException(sm.getString("clusteredSession.isNew.ise"));

        return (this.isNew);
    }

    @Override
    public void setNew(boolean isNew) {
        this.isNew = isNew;

        // Don't replicate metadata just 'cause its the second request
        // The only effect of this is if someone besides a request
        // deserializes metadata from the distributed cache, this
        // field may be out of date.
        // If a request accesses the session, the access() call will
        // set isNew=false, so the request will see the correct value
        // sessionMetadataDirty();
    }

    /**
     * Overrides the {@link StandardSession#isValid() superclass method} to call @ #isValid(boolean) isValid(true)} .
     */
    @Override
    public boolean isValid() {
        return isValid(true);
    }

    @Override
    public void setValid(boolean isValid) {
        this.isValid = isValid;
        sessionMetadataDirty();
    }

    /**
     * Invalidates this session and unbinds any objects bound to it. Overridden here to remove across the cluster instead of
     * just expiring.
     *
     * @exception IllegalStateException if this method is called on an invalidated session
     */
    @Override
    public void invalidate() {
        if (!isValid())
            throw new IllegalStateException(sm.getString("clusteredSession.invalidate.ise"));

        // Cause this session to expire globally
        boolean notify = true;
        boolean localCall = true;
        boolean localOnly = false;
        expire(notify, localCall, localOnly, ClusteredSessionNotificationCause.INVALIDATE);
        // Preemptively relinquish ownership that was acquired in access() - don't wait for endAccess()
        this.relinquishSessionOwnership(false);
    }

    @Override
    public void expire() {
        boolean notify = true;
        boolean localCall = true;
        boolean localOnly = false;
        expire(notify, localCall, localOnly, ClusteredSessionNotificationCause.INVALIDATE);
    }

    @Override
    public void recycle() {
        if (!isValid) {
            // Thread no longer needs to track this session
            SessionInvalidationTracker.clearInvalidatedSession(id, manager);
        }

        // Reset the instance variables associated with this Session
        attributes.clear();
        setAuthType(null);
        creationTime = 0L;
        expiring = false;
        id = null;
        lastAccessedTime = 0L;
        maxInactiveInterval = -1;
        notes.clear();
        setPrincipal(null);
        isNew = false;
        isValid = false;
        firstAccess = true;
        manager = null;

        listeners.clear();
        support = new PropertyChangeSupport(this);

        invalidationPolicy = ReplicationTrigger.ACCESS;
        outdatedTime = 0;
        sessionAttributesDirty = false;
        sessionMetadataDirty = false;
        realId = null;
        version.set(0);
        hasActivationListener = null;
        lastReplicated = 0;
        maxUnreplicatedInterval = 0;
        alwaysReplicateTimestamp = true;
        this.notificationPolicy = null;
        this.clusterStatus = null;
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Object getNote(String name) {
        return (notes.get(name));
    }

    @Override
    public Iterator<String> getNoteNames() {
        return (notes.keySet().iterator());
    }

    @Override
    public void setNote(String name, Object value) {
        notes.put(name, value);
    }

    @Override
    public void removeNote(String name) {
        notes.remove(name);
    }

    // TODO uncomment when work on JBAS-1900 is completed
    // public void removeNote(String name)
    // {
    // // FormAuthenticator removes the username and password because
    // // it assumes they are not needed if the Principal is cached,
    // // but they are needed if the session fails over, so ignore
    // // the removal request.
    // // TODO discuss this on Tomcat dev list to see if a better
    // // way of handling this can be found
    // if (Constants.SESS_USERNAME_NOTE.equals(name)
    // || Constants.SESS_PASSWORD_NOTE.equals(name))
    // {
    // if (log.isDebugEnabled())
    // {
    // log.debug("removeNote(): ignoring removal of note " + name);
    // }
    // }
    // else
    // {
    // super.removeNote(name);
    // }
    //
    // }

    // TODO uncomment when work on JBAS-1900 is completed
    // public void setNote(String name, Object value)
    // {
    // super.setNote(name, value);
    //
    // if (Constants.SESS_USERNAME_NOTE.equals(name)
    // || Constants.SESS_PASSWORD_NOTE.equals(name))
    // {
    // sessionIsDirty();
    // }
    // }

    @Override
    public HttpSession getSession() {
        if (facade == null) {
            if (SecurityUtil.isPackageProtectionEnabled()) {
                final HttpSession fsession = this;
                StandardSessionFacade ssf = AccessController.doPrivileged(new PrivilegedAction<StandardSessionFacade>() {
                    @Override
                    public StandardSessionFacade run() {
                        return new StandardSessionFacade(fsession);
                    }
                });
                this.facade = ssf;
            } else {
                facade = new StandardSessionFacade(this);
            }
        }
        return (facade);
    }

    // ------------------------------------------------------------ HttpSession

    @Override
    public ServletContext getServletContext() {
        if (manager == null)
            return (null);
        Context context = (Context) manager.getContainer();
        if (context == null)
            return (null);
        else
            return (context.getServletContext());
    }

    @Override
    public Object getAttribute(String name) {
        if (!isValid())
            throw new IllegalStateException(sm.getString("clusteredSession.getAttribute.ise"));

        return getAttributeInternal(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<String> getAttributeNames() {
        if (!isValid())
            throw new IllegalStateException(sm.getString("clusteredSession.getAttributeNames.ise"));

        return (new Enumerator(getAttributesInternal().keySet(), true));
    }

    @Override
    public void setAttribute(String name, Object value) {
        // Name cannot be null
        if (name == null)
            throw new IllegalArgumentException(sm.getString("clusteredSession.setAttribute.namenull"));

        // Null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(name);
            return;
        }

        // Validate our current state
        if (!isValidInternal()) {
            throw new IllegalStateException(sm.getString("clusteredSession.setAttribute.ise"));
        }

        if (canAttributeBeReplicated(value) == false) {
            throw new IllegalArgumentException(sm.getString("clusteredSession.setAttribute.iae"));
        }

        // Construct an event with the new value
        HttpSessionBindingEvent event = null;

        // Call the valueBound() method if necessary
        if (value instanceof HttpSessionBindingListener
                && notificationPolicy.isHttpSessionBindingListenerInvocationAllowed(this.clusterStatus,
                        ClusteredSessionNotificationCause.MODIFY, name, true)) {
            event = new HttpSessionBindingEvent(getSession(), name, value);
            try {
                ((HttpSessionBindingListener) value).valueBound(event);
            } catch (Throwable t) {
                manager.getContainer().getLogger().error(sm.getString("clusteredSession.bindingEvent"), t);
            }
        }

        if (value instanceof HttpSessionActivationListener)
            hasActivationListener = Boolean.TRUE;

        // Replace or add this attribute
        Object unbound = setAttributeInternal(name, value);

        // Call the valueUnbound() method if necessary
        if ((unbound != null)
                && (unbound != value)
                && (unbound instanceof HttpSessionBindingListener)
                && notificationPolicy.isHttpSessionBindingListenerInvocationAllowed(this.clusterStatus,
                        ClusteredSessionNotificationCause.MODIFY, name, true)) {
            try {
                ((HttpSessionBindingListener) unbound).valueUnbound(new HttpSessionBindingEvent(getSession(), name));
            } catch (Throwable t) {
                manager.getContainer().getLogger().error(sm.getString("clusteredSession.bindingEvent"), t);
            }
        }

        // Notify interested application event listeners
        if (notificationPolicy.isHttpSessionAttributeListenerInvocationAllowed(this.clusterStatus,
                ClusteredSessionNotificationCause.MODIFY, name, true)) {
            Context context = (Context) manager.getContainer();
            Object[] lifecycleListeners = context.getApplicationEventListeners();
            if (lifecycleListeners == null)
                return;
            for (int i = 0; i < lifecycleListeners.length; i++) {
                if (!(lifecycleListeners[i] instanceof HttpSessionAttributeListener))
                    continue;
                HttpSessionAttributeListener listener = (HttpSessionAttributeListener) lifecycleListeners[i];
                try {
                    if (unbound != null) {
                        fireContainerEvent(context, "beforeSessionAttributeReplaced", listener);
                        if (event == null) {
                            event = new HttpSessionBindingEvent(getSession(), name, unbound);
                        }
                        listener.attributeReplaced(event);
                        fireContainerEvent(context, "afterSessionAttributeReplaced", listener);
                    } else {
                        fireContainerEvent(context, "beforeSessionAttributeAdded", listener);
                        if (event == null) {
                            event = new HttpSessionBindingEvent(getSession(), name, value);
                        }
                        listener.attributeAdded(event);
                        fireContainerEvent(context, "afterSessionAttributeAdded", listener);
                    }
                } catch (Throwable t) {
                    try {
                        if (unbound != null) {
                            fireContainerEvent(context, "afterSessionAttributeReplaced", listener);
                        } else {
                            fireContainerEvent(context, "afterSessionAttributeAdded", listener);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    manager.getContainer().getLogger().error(sm.getString("clusteredSession.attributeEvent"), t);
                }
            }
        }
    }

    @Override
    public void removeAttribute(String name) {
        // Validate our current state
        if (!isValidInternal())
            throw new IllegalStateException(sm.getString("clusteredSession.removeAttribute.ise"));

        final boolean localCall = true;
        final boolean localOnly = false;
        final boolean notify = true;
        removeAttributeInternal(name, localCall, localOnly, notify, ClusteredSessionNotificationCause.MODIFY);
    }

    @Override
    @SuppressWarnings("deprecation")
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        return (sessionContext);
    }

    @Override
    public Object getValue(String name) {
        return (getAttribute(name));
    }

    @Override
    public String[] getValueNames() {
        if (!isValidInternal())
            throw new IllegalStateException(sm.getString("clusteredSession.getValueNames.ise"));

        return (keys());
    }

    @Override
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeValue(String name) {
        removeAttribute(name);
    }

    // ---------------------------------------------------- DistributableSession

    /**
     * Gets the session id with any appended jvmRoute info removed.
     *
     * @see #getUseJK()
     */
    public String getRealId() {
        return realId;
    }

    public boolean getMustReplicateTimestamp() {
        // If the access times are the same, access() was never called
        // on the session
        boolean touched = this.thisAccessedTime != this.lastAccessedTime;
        boolean exceeds = alwaysReplicateTimestamp && touched;

        if (!exceeds && touched && maxUnreplicatedInterval > 0) // -1 means ignore
        {
            long unrepl = System.currentTimeMillis() - lastReplicated;
            exceeds = (unrepl >= maxUnreplicatedInterval);
        }

        return exceeds;
    }

    /**
     * {@inheritDoc}
     */
    public void update(IncomingDistributableSessionData sessionData) {
        assert sessionData != null : "sessionData is null";

        this.version.set(sessionData.getVersion());

        long ts = sessionData.getTimestamp();
        this.lastAccessedTime = this.thisAccessedTime = ts;
        this.timestamp.set(ts);

        DistributableSessionMetadata md = sessionData.getMetadata();
        // TODO -- get rid of these field and delegate to metadata
        this.id = md.getId();
        this.creationTime = md.getCreationTime();
        this.maxInactiveInterval = md.getMaxInactiveInterval();
        this.isNew = md.isNew();
        this.isValid = md.isValid();
        this.metadata = md;

        // Get our id without any jvmRoute appended
        parseRealId(id);

        // We no longer know if we have an activationListener
        hasActivationListener = null;

        // If the session has been replicated, any subsequent
        // access cannot be the first.
        this.firstAccess = false;

        // We don't know when we last replicated our timestamp. We may be
        // getting called due to activation, not deserialization after
        // replication, so this.timestamp may be after the last replication.
        // So use the creation time as a conservative guesstimate. Only downside
        // is we may replicate a timestamp earlier than we need to, which is not
        // a heavy cost.
        this.lastReplicated = this.creationTime;

        this.clusterStatus = new ClusteredSessionManagementStatus(this.realId, true, null, null);

        checkAlwaysReplicateTimestamp();

        populateAttributes(sessionData.getSessionAttributes());

        // TODO uncomment when work on JBAS-1900 is completed
        // // Session notes -- for FORM auth apps, allow replicated session
        // // to be used without requiring a new login
        // // We use the superclass set/removeNote calls here to bypass
        // // the custom logic we've added
        // String username = (String) in.readObject();
        // if (username != null)
        // {
        // super.setNote(Constants.SESS_USERNAME_NOTE, username);
        // }
        // else
        // {
        // super.removeNote(Constants.SESS_USERNAME_NOTE);
        // }
        // String password = (String) in.readObject();
        // if (password != null)
        // {
        // super.setNote(Constants.SESS_PASSWORD_NOTE, password);
        // }
        // else
        // {
        // super.removeNote(Constants.SESS_PASSWORD_NOTE);
        // }

        // We are no longer outdated vis a vis distributed cache
        this.outdatedTime = 0;

        // Requests must publish our full state back to the cluster in case anything got dropped.
        this.requireFullReplication();
    }

    // ------------------------------------------------------------------ Public

    /**
     * Increment our version and propagate ourself to the distributed cache.
     */
    public synchronized void processSessionReplication() {
        // Replicate the session.
        if (log.isTraceEnabled()) {
            log.trace("processSessionReplication(): session is dirty. Will increment " + "version from: " + getVersion()
                    + " and replicate.");
        }
        version.incrementAndGet();

        O outgoingData = getOutgoingSessionData();
        distributedCacheManager.storeSessionData(outgoingData);

        sessionAttributesDirty = false;
        sessionMetadataDirty = false;

        lastReplicated = System.currentTimeMillis();

        this.fullReplicationRequired = false;
        if (this.fullReplicationWindow > 0 && System.currentTimeMillis() > this.fullReplicationWindow) {
            this.fullReplicationWindow = -1;
        }
    }

    /**
     * Remove myself from the distributed cache.
     */
    public void removeMyself() {
        getDistributedCacheManager().removeSession(getRealId());
    }

    /**
     * Remove myself from the <t>local</t> instance of the distributed cache.
     */
    public void removeMyselfLocal() {
        getDistributedCacheManager().removeSessionLocal(getRealId());
    }

    /**
     * Gets the sessions creation time, skipping any validity check.
     *
     * @return the creation time
     */
    public long getCreationTimeInternal() {
        return creationTime;
    }

    /**
     * Gets the time {@link #processSessionReplication()} was last called, or <code>0</code> if it has never been called.
     */
    public long getLastReplicated() {
        return lastReplicated;
    }

    /**
     * Gets the maximum period in ms after which a request accessing this session will trigger replication of its timestamp,
     * even if the request doesn't otherwise modify the session. A value of -1 means no limit.
     */
    public long getMaxUnreplicatedInterval() {
        return maxUnreplicatedInterval;
    }

    /**
     * Sets the maximum period in ms after which a request accessing this session will trigger replication of its timestamp,
     * even if the request doesn't otherwise modify the session. A value of -1 means no limit.
     */
    public void setMaxUnreplicatedInterval(long interval) {
        this.maxUnreplicatedInterval = Math.max(interval, -1);
        checkAlwaysReplicateTimestamp();
    }

    /**
     * This is called specifically for failover case using mod_jk where the new session has this node name in there. As a
     * result, it is safe to just replace the id since the backend store is using the "real" id without the node name.
     *
     * @param id
     */
    public void resetIdWithRouteInfo(String id) {
        this.id = id;
        parseRealId(id);
    }

    /**
     * Update our version due to changes in the distributed cache.
     *
     * @param version the distributed cache version
     * @return <code>true</code>
     */
    public boolean setVersionFromDistributedCache(int version) {
        boolean outdated = getVersion() < version;
        if (outdated) {
            outdatedTime = System.currentTimeMillis();
        }
        return outdated;
    }

    /**
     * Check to see if the session data is still valid. Outdated here means that the in-memory data is not in sync with one in
     * the data store.
     *
     * @return
     */
    public boolean isOutdated() {
        // if creationTime == 0 we've neither been synced with the
        // distributed cache nor had creation time set (i.e. brand new session)
        return thisAccessedTime < outdatedTime || this.creationTime == 0;
    }

    public boolean isSessionDirty() {
        return sessionAttributesDirty || sessionMetadataDirty;
    }

    /** Inform any HttpSessionListener of the creation of this session */
    public void tellNew(ClusteredSessionNotificationCause cause) {
        // Notify interested session event listeners
        fireSessionEvent(Session.SESSION_CREATED_EVENT, null);

        // Notify interested application event listeners
        if (notificationPolicy.isHttpSessionListenerInvocationAllowed(this.clusterStatus, cause, true)) {
            Context context = (Context) manager.getContainer();
            Object[] lifecycleListeners = context.getApplicationSessionLifecycleListeners();
            if (lifecycleListeners != null) {
                HttpSessionEvent event = new HttpSessionEvent(getSession());
                for (int i = 0; i < lifecycleListeners.length; i++) {
                    if (!(lifecycleListeners[i] instanceof HttpSessionListener))
                        continue;
                    HttpSessionListener listener = (HttpSessionListener) lifecycleListeners[i];
                    try {
                        fireContainerEvent(context, "beforeSessionCreated", listener);
                        listener.sessionCreated(event);
                        fireContainerEvent(context, "afterSessionCreated", listener);
                    } catch (Throwable t) {
                        try {
                            fireContainerEvent(context, "afterSessionCreated", listener);
                        } catch (Exception e) {
                            // Ignore
                        }
                        manager.getContainer().getLogger().error(sm.getString("clusteredSession.sessionEvent"), t);
                    }
                }
            }
        }
    }

    /**
     * Returns whether the current session is still valid, but only calls <code>expire</code> for timed-out sessions if
     * <code>expireIfInvalid</code> is <code>true</code>.
     *
     * @param expireIfInvalid <code>true</code> if sessions that have been timed out should be expired
     */
    public boolean isValid(boolean expireIfInvalid) {
        if (this.expiring) {
            return true;
        }

        if (!this.isValid) {
            return false;
        }

        if (ACTIVITY_CHECK && accessCount.get() > 0) {
            return true;
        }

        if (maxInactiveInterval > 0) {
            long timeNow = System.currentTimeMillis();
            int timeIdle = (int) ((timeNow - thisAccessedTime) / 1000L);
            if (timeIdle >= maxInactiveInterval) {
                if (expireIfInvalid) {
                    boolean notify = true;
                    boolean localCall = true;
                    boolean localOnly = true;
                    expire(notify, localCall, localOnly, ClusteredSessionNotificationCause.TIMEOUT);
                } else {
                    return false;
                }
            }
        }

        return (this.isValid);

    }

    /**
     * Expires the session, notifying listeners and possibly the manager.
     * <p>
     * <strong>NOTE:</strong> The manager will only be notified of the expiration if <code>localCall</code> is <code>true</code>
     * ; otherwise it is the responsibility of the caller to notify the manager that the session is expired. (In the case of
     * JBossCacheManager, it is the manager itself that makes such a call, so it of course is aware).
     * </p>
     *
     * @param notify whether servlet spec listeners should be notified
     * @param localCall <code>true</code> if this call originated due to local activity (such as a session invalidation in user
     *        code or an expiration by the local background processing thread); <code>false</code> if the expiration originated
     *        due to some kind of event notification from the cluster.
     * @param localOnly <code>true</code> if the expiration should not be announced to the cluster, <code>false</code> if other
     *        cluster nodes should be made aware of the expiration. Only meaningful if <code>localCall</code> is
     *        <code>true</code>.
     * @param cause the cause of the expiration
     */
    public void expire(boolean notify, boolean localCall, boolean localOnly, ClusteredSessionNotificationCause cause) {
        if (log.isTraceEnabled()) {
            log.trace("The session has expired with id: " + id + " -- is expiration local? " + localOnly);
        }

        // If another thread is already doing this, stop
        if (expiring)
            return;

        synchronized (this) {
            // If we had a race to this sync block, another thread may
            // have already completed expiration. If so, don't do it again
            if (!isValid)
                return;

            if (manager == null)
                return;

            expiring = true;

            // SRV.10.6 (2.5) 11.6 (3.0) Propagate listener exceptions
            RuntimeException listenerException = null;

            if (localCall) {
                this.acquireSessionOwnership();
            }

            try {
                // Notify interested application event listeners
                // FIXME - Assumes we call listeners in reverse order
                Context context = (Context) manager.getContainer();
                Object[] lifecycleListeners = context.getApplicationSessionLifecycleListeners();
                if (notify && (lifecycleListeners != null)
                        && notificationPolicy.isHttpSessionListenerInvocationAllowed(this.clusterStatus, cause, localCall)) {
                    HttpSessionEvent event = new HttpSessionEvent(getSession());
                    for (int i = 0; i < lifecycleListeners.length; i++) {
                        int j = (lifecycleListeners.length - 1) - i;
                        if (!(lifecycleListeners[j] instanceof HttpSessionListener))
                            continue;
                        HttpSessionListener listener = (HttpSessionListener) lifecycleListeners[j];
                        try {
                            fireContainerEvent(context, "beforeSessionDestroyed", listener);
                            try {
                                listener.sessionDestroyed(event);
                            } catch (RuntimeException e) {
                                if (listenerException == null) {
                                    listenerException = e;
                                }
                            }
                            fireContainerEvent(context, "afterSessionDestroyed", listener);
                        } catch (Throwable t) {
                            try {
                                fireContainerEvent(context, "afterSessionDestroyed", listener);
                            } catch (Exception e) {
                                // Ignore
                            }
                            manager.getContainer().getLogger().error(sm.getString("clusteredSession.sessionEvent"), t);
                        }
                    }
                }

                if (ACTIVITY_CHECK) {
                    accessCount.set(0);
                }

                // Notify interested session event listeners.
                if (notify) {
                    fireSessionEvent(Session.SESSION_DESTROYED_EVENT, null);
                }

                // JBAS-1360 -- Unbind any objects associated with this session
                String[] keys = keys();
                for (int i = 0; i < keys.length; i++) {
                    try {
                        removeAttributeInternal(keys[i], localCall, localOnly, notify, cause);
                    } catch (RuntimeException e) {
                        if (listenerException == null) {
                            listenerException = e;
                        }
                    }
                }

                // Remove this session from our manager's active sessions
                // If !localCall, this expire call came from the manager,
                // so don't recurse
                if (localCall) {
                    removeFromManager(localOnly);
                }

                if (listenerException != null) {
                    throw listenerException;
                }
            } finally {
                // We have completed expire of this session
                setValid(false);
                expiring = false;
                if (localCall) {
                    this.relinquishSessionOwnership(true);
                }
            }
        }
    }

    /**
     * Inform any HttpSessionActivationListener that the session will passivate.
     *
     * @param cause cause of the notification (e.g. {@link ClusteredSessionNotificationCause#REPLICATION} or
     *        {@link ClusteredSessionNotificationCause#PASSIVATION}
     */
    public void notifyWillPassivate(ClusteredSessionNotificationCause cause) {
        // Notify interested session event listeners
        fireSessionEvent(Session.SESSION_PASSIVATED_EVENT, null);

        if (hasActivationListener != Boolean.FALSE) {
            boolean hasListener = false;

            // Notify ActivationListeners
            HttpSessionEvent event = null;
            String[] keys = keys();
            Map<String, Object> attrs = getAttributesInternal();
            for (int i = 0; i < keys.length; i++) {
                Object attribute = attrs.get(keys[i]);
                if (attribute instanceof HttpSessionActivationListener) {
                    hasListener = true;

                    if (notificationPolicy.isHttpSessionActivationListenerInvocationAllowed(this.clusterStatus, cause, keys[i])) {
                        if (event == null)
                            event = new HttpSessionEvent(getSession());

                        try {
                            ((HttpSessionActivationListener) attribute).sessionWillPassivate(event);
                        } catch (Throwable t) {
                            manager.getContainer().getLogger().error(sm.getString("clusteredSession.attributeEvent"), t);
                        }
                    }
                }
            }

            hasActivationListener = hasListener ? Boolean.TRUE : Boolean.FALSE;
        }

        if (cause != ClusteredSessionNotificationCause.PASSIVATION) {
            this.needsPostReplicateActivation = true;
        }
    }

    /**
     * Inform any HttpSessionActivationListener that the session has been activated.
     *
     * @param cause cause of the notification (e.g. {@link ClusteredSessionNotificationCause#REPLICATION} or
     *        {@link ClusteredSessionNotificationCause#PASSIVATION}
     */
    public void notifyDidActivate(ClusteredSessionNotificationCause cause) {
        if (cause == ClusteredSessionNotificationCause.ACTIVATION) {
            this.needsPostReplicateActivation = true;
        }

        // Notify interested session event listeners
        fireSessionEvent(Session.SESSION_ACTIVATED_EVENT, null);

        if (hasActivationListener != Boolean.FALSE) {
            // Notify ActivationListeners

            boolean hasListener = false;

            HttpSessionEvent event = null;
            String[] keys = keys();
            Map<String, Object> attrs = getAttributesInternal();
            for (int i = 0; i < keys.length; i++) {
                Object attribute = attrs.get(keys[i]);
                if (attribute instanceof HttpSessionActivationListener) {
                    hasListener = true;

                    if (notificationPolicy.isHttpSessionActivationListenerInvocationAllowed(this.clusterStatus, cause, keys[i])) {
                        if (event == null)
                            event = new HttpSessionEvent(getSession());
                        try {
                            ((HttpSessionActivationListener) attribute).sessionDidActivate(event);
                        } catch (Throwable t) {
                            manager.getContainer().getLogger().error(sm.getString("clusteredSession.attributeEvent"), t);
                        }
                    }
                }
            }

            hasActivationListener = hasListener ? Boolean.TRUE : Boolean.FALSE;
        }

        if (cause != ClusteredSessionNotificationCause.ACTIVATION) {
            this.needsPostReplicateActivation = false;
        }
    }

    /**
     * Gets whether the session needs to notify HttpSessionActivationListeners that it has been activated following replication.
     */
    public boolean getNeedsPostReplicateActivation() {
        return needsPostReplicateActivation;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append("id: ").append(id)
                .append(" lastAccessedTime: ").append(lastAccessedTime).append(" version: ").append(version)
                .append(" lastOutdated: ").append(outdatedTime).append(']').toString();
    }

    // ----------------------------------------------------- Protected Methods

    protected abstract Object setAttributeInternal(String name, Object value);

    protected abstract Object removeAttributeInternal(String name, boolean localCall, boolean localOnly);

    protected abstract O getOutgoingSessionData();

    protected Object getAttributeInternal(String name) {
        Object result = getAttributesInternal().get(name);

        // Do dirty check even if result is null, as w/ SET_AND_GET null
        // still makes us dirty (ensures timely replication w/o using ACCESS)
        if (isGetDirty(result)) {
            sessionAttributesDirty();
        }

        return result;
    }

    /**
     * Extension point for subclasses to load the attribute map from the distributed cache.
     */
    protected void populateAttributes(Map<String, Object> distributedCacheAttributes) {
        Map<String, Object> existing = getAttributesInternal();
        Map<String, Object> excluded = removeExcludedAttributes(existing);

        existing.clear();

        existing.putAll(distributedCacheAttributes);
        if (excluded != null)
            existing.putAll(excluded);
    }

    protected final Map<String, Object> getAttributesInternal() {
        return attributes;
    }

    protected final ClusteredSessionManager<O> getManagerInternal() {
        return manager;
    }

    protected final DistributedCacheManager<O> getDistributedCacheManager() {
        return distributedCacheManager;
    }

    protected final void setDistributedCacheManager(DistributedCacheManager<O> distributedCacheManager) {
        this.distributedCacheManager = distributedCacheManager;
    }

    /**
     * Returns whether the attribute's type is one that can be replicated.
     *
     * @param attribute the attribute
     * @return <code>true</code> if <code>attribute</code> is <code>null</code>, <code>Serializable</code> or an array of
     *         primitives.
     */
    protected boolean canAttributeBeReplicated(Object attribute) {
        if (attribute instanceof Serializable || attribute == null)
            return true;
        Class<?> clazz = attribute.getClass().getComponentType();
        return (clazz != null && clazz.isPrimitive());
    }

    /**
     * Removes any attribute whose name is found in {@link #excludedAttributes} from <code>attributes</code> and returns a Map
     * of all such attributes.
     *
     * @param attributes source map from which excluded attributes are to be removed.
     *
     * @return Map that contains any attributes removed from <code>attributes</code>, or <code>null</code> if no attributes were
     *         removed.
     */
    protected final Map<String, Object> removeExcludedAttributes(Map<String, Object> attributes) {
        Map<String, Object> excluded = null;
        for (int i = 0; i < excludedAttributes.length; i++) {
            Object attr = attributes.remove(excludedAttributes[i]);
            if (attr != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Excluding attribute " + excludedAttributes[i] + " from replication");
                }
                if (excluded == null) {
                    excluded = new HashMap<String, Object>();
                }
                excluded.put(excludedAttributes[i], attr);
            }
        }

        return excluded;
    }

    protected final boolean isGetDirty(Object attribute) {
        boolean result = false;
        switch (invalidationPolicy) {
            case SET_AND_GET:
                result = true;
                break;
            case SET_AND_NON_PRIMITIVE_GET:
                result = isMutable(attribute);
                break;
            default:
                // result is false
        }
        return result;
    }

    protected boolean isMutable(Object attribute) {
        return attribute != null
                && !(attribute instanceof String || attribute instanceof Number || attribute instanceof Character || attribute instanceof Boolean);
    }

    /**
     * Gets a reference to the JBossCacheService.
     */
    protected void establishDistributedCacheManager() {
        if (distributedCacheManager == null) {
            distributedCacheManager = getManagerInternal().getDistributedCacheManager();

            // still null???
            if (distributedCacheManager == null) {
                throw new RuntimeException("DistributedCacheManager is null.");
            }
        }
    }

    protected final void sessionAttributesDirty() {
        if (!sessionAttributesDirty && log.isTraceEnabled())
            log.trace("Marking session attributes dirty " + id);

        sessionAttributesDirty = true;
    }

    protected final void setHasActivationListener(boolean hasListener) {
        this.hasActivationListener = Boolean.valueOf(hasListener);
    }

    protected int getVersion() {
        return version.get();
    }

    protected long getSessionTimestamp() {
        this.timestamp.set(this.thisAccessedTime);
        return this.timestamp.get();
    }

    protected boolean isSessionMetadataDirty() {
        return sessionMetadataDirty;
    }

    protected DistributableSessionMetadata getSessionMetadata() {
        this.metadata.setId(id);
        this.metadata.setCreationTime(creationTime);
        this.metadata.setMaxInactiveInterval(maxInactiveInterval);
        this.metadata.setNew(isNew);
        this.metadata.setValid(isValid);

        return this.metadata;
    }

    protected boolean isSessionAttributeMapDirty() {
        return sessionAttributesDirty || isFullReplicationNeeded();
    }

    protected boolean isFullReplicationNeeded() {
        if (fullReplicationRequired) {
            return true;
        }
        return fullReplicationRequired || (fullReplicationWindow > 0 && System.currentTimeMillis() < fullReplicationWindow);
    }

    // ----------------------------------------------------------------- Private

    private void checkAlwaysReplicateTimestamp() {
        this.alwaysReplicateTimestamp = (maxUnreplicatedInterval == 0 || (maxUnreplicatedInterval > 0
                && maxInactiveInterval >= 0 && maxUnreplicatedInterval > (maxInactiveInterval * 1000)));
    }

    private void parseRealId(String sessionId) {
        String newId = this.manager.parse(sessionId).getKey();

        // realId is used in a lot of map lookups, so only replace it
        // if the new id is actually different -- preserve object identity
        if (!newId.equals(realId)) {
            realId = newId;
        }
    }

    /**
     * Remove the attribute from the local cache and possibly the distributed cache, plus notify any listeners
     *
     * @param name the attribute name
     * @param localCall <code>true</code> if this call originated from local activity (e.g. a removeAttribute() in the webapp or
     *        a local session invalidation/expiration), <code>false</code> if it originated due to an remote event in the
     *        distributed cache.
     * @param localOnly <code>true</code> if the removal should not be replicated around the cluster
     * @param notify <code>true</code> if listeners should be notified
     * @param cause the cause of the removal
     */
    private void removeAttributeInternal(String name, boolean localCall, boolean localOnly, boolean notify,
            ClusteredSessionNotificationCause cause) {
        // Remove this attribute from our collection
        Object value = removeAttributeInternal(name, localCall, localOnly);

        // Do we need to do valueUnbound() and attributeRemoved() notification?
        if (!notify || (value == null)) {
            return;
        }

        // Call the valueUnbound() method if necessary
        HttpSessionBindingEvent event = null;
        if (value instanceof HttpSessionBindingListener
                && notificationPolicy.isHttpSessionBindingListenerInvocationAllowed(this.clusterStatus, cause, name, localCall)) {
            event = new HttpSessionBindingEvent(getSession(), name, value);
            ((HttpSessionBindingListener) value).valueUnbound(event);
        }

        // Notify interested application event listeners
        if (notificationPolicy.isHttpSessionAttributeListenerInvocationAllowed(this.clusterStatus, cause, name, localCall)) {
            Context context = (Context) manager.getContainer();
            Object[] lifecycleListeners = context.getApplicationEventListeners();
            if (lifecycleListeners != null) {
                // SRV.10.6 (2.5) 11.6 (3.0) Propagate listener exceptions
                RuntimeException listenerException = null;

                for (int i = 0; i < lifecycleListeners.length; i++) {
                    if (!(lifecycleListeners[i] instanceof HttpSessionAttributeListener))
                        continue;
                    HttpSessionAttributeListener listener = (HttpSessionAttributeListener) lifecycleListeners[i];
                    try {
                        fireContainerEvent(context, "beforeSessionAttributeRemoved", listener);
                        if (event == null) {
                            event = new HttpSessionBindingEvent(getSession(), name, value);
                        }
                        try {
                            listener.attributeRemoved(event);
                        } catch (RuntimeException e) {
                            if (listenerException == null) {
                                listenerException = e;
                            }
                        }
                        fireContainerEvent(context, "afterSessionAttributeRemoved", listener);
                    } catch (Throwable t) {
                        try {
                            fireContainerEvent(context, "afterSessionAttributeRemoved", listener);
                        } catch (Exception e) {
                            // Ignore
                        }
                        manager.getContainer().getLogger().error(sm.getString("clusteredSession.attributeEvent"), t);
                    }
                }

                if (listenerException != null) {
                    throw listenerException;
                }
            }
        }
    }

    private String[] keys() {
        Set<String> keySet = getAttributesInternal().keySet();
        return ((String[]) keySet.toArray(new String[keySet.size()]));
    }

    /**
     * Return the <code>isValid</code> flag for this session without any expiration check.
     */
    @Override
    public boolean isValidInternal() {
        return this.isValid || this.expiring;
    }

    /**
     * Fire container events if the Context implementation is the <code>org.apache.catalina.core.StandardContext</code>.
     *
     * @param context Context for which to fire events
     * @param type Event type
     * @param data Event data
     *
     * @exception Exception occurred during event firing
     */
    private void fireContainerEvent(Context context, String type, Object data) throws Exception {

        if (!"org.apache.catalina.core.StandardContext".equals(context.getClass().getName())) {
            return; // Container events are not supported
        }
        // NOTE: Race condition is harmless, so do not synchronize
        if (containerEventMethod == null) {
            containerEventMethod = context.getClass().getMethod("fireContainerEvent", containerEventTypes);
        }
        Object[] containerEventParams = new Object[2];
        containerEventParams[0] = type;
        containerEventParams[1] = data;
        containerEventMethod.invoke(context, containerEventParams);

    }

    /**
     * Notify all session event listeners that a particular event has occurred for this Session. The default implementation
     * performs this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    private void fireSessionEvent(String type, Object data) {
        if (listeners.size() < 1)
            return;
        SessionEvent event = new SessionEvent(this, type, data);
        SessionListener[] list = new SessionListener[0];
        synchronized (listeners) {
            list = (SessionListener[]) listeners.toArray(list);
        }

        for (int i = 0; i < list.length; i++) {
            ((SessionListener) list[i]).sessionEvent(event);
        }

    }

    private void sessionMetadataDirty() {
        if (!sessionMetadataDirty && !isNew && log.isTraceEnabled())
            log.trace("Marking session metadata dirty " + id);
        sessionMetadataDirty = true;
    }

    /**
     * Advise our manager to remove this expired session.
     *
     * @param localOnly whether the rest of the cluster should be made aware of the removal
     */
    private void removeFromManager(boolean localOnly) {
        if (localOnly) {
            manager.removeLocal(this);
        } else {
            manager.remove(this);
        }
    }

    private void requireFullReplication() {
        this.fullReplicationRequired = true;
        this.fullReplicationWindow = System.currentTimeMillis() + FULL_REPLICATION_WINDOW_LENGTH;
    }

}
