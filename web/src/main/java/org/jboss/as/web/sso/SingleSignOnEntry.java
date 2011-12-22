/*
 * Copyright 1999-2001,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.web.sso;

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.SingleSignOn;

/**
 * A class that represents entries in the cache of authenticated users.
 *
 * @author Brian E. Stansberry, based on work by Craig R. McClanahan
 * @see SingleSignOn
 */
public class SingleSignOnEntry extends org.apache.catalina.authenticator.SingleSignOnEntry {

    public SingleSignOnEntry(Principal principal, String authType, String username, String password) {
        super(principal, authType, username, password);
    }

    /**
     * Adds a <code>Session</code> to the list of those associated with this SSO.
     *
     * @param sso The <code>SingleSignOn</code> valve that is managing the SSO session.
     * @param session The <code>Session</code> being associated with the SSO.
     * @return <code>true</code> if the given Session was a new addition (i.e. was not previously associated with this entry);
     *         <code>false</code> otherwise.
     */
    synchronized boolean addSession2(SingleSignOn sso, Session session) {
        for (int i = 0; i < sessions.length; i++) {
            if (session == sessions[i]) {
                return false;
            }
        }
        Session[] results = new Session[sessions.length + 1];
        System.arraycopy(sessions, 0, results, 0, sessions.length);
        results[sessions.length] = session;
        sessions = results;
        session.addSessionListener(sso);
        return true;
    }

    /**
     * Removes the given <code>Session</code> from the list of those associated with this SSO.
     *
     * @param session the <code>Session</code> to remove.
     * @return <code>true</code> if the given Session needed to be removed (i.e. was in fact previously associated with this
     *         entry); <code>false</code> otherwise.
     */
    synchronized boolean removeSession2(Session session) {
        if (sessions.length == 0) {
            return false;
        }
        boolean removed = false;
        Session[] nsessions = new Session[sessions.length - 1];
        for (int i = 0, j = 0; i < sessions.length; i++) {
            if (session == sessions[i]) {
                removed = true;
                continue;
            } else if (!removed && i == nsessions.length) {
                // We have tested all our sessions, and have not had to
                // remove any; break loop now so we don't cause an
                // ArrayIndexOutOfBounds on nsessions
                break;
            }
            nsessions[j++] = sessions[i];
        }

        // Only if we removed a session, do we replace our session list
        if (removed) {
            sessions = nsessions;
        }
        return removed;
    }

    /**
     * Sets the <code>Principal</code> that has been authenticated by the SSO.
     */
    void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    /**
     * Returns the number of sessions associated with this SSO, either locally or remotely.
     */
    int getSessionCount() {
        return sessions.length;
    }

    @Override
    public void updateCredentials(Principal principal, String authType, String username, String password) {
        updateCredentials2(principal, authType, username, password);
    }

    /**
     * Updates the SingleSignOnEntry to reflect the latest security information associated with the caller.
     *
     * @param principal the <code>Principal</code> returned by the latest call to <code>Realm.authenticate</code>.
     * @param authType the type of authenticator used (BASIC, CLIENT-CERT, DIGEST or FORM)
     * @param username the username (if any) used for the authentication
     * @param password the password (if any) used for the authentication
     */
    public synchronized boolean updateCredentials2(Principal principal, String authType, String username, String password) {

        boolean changed = (safeEquals(this.principal, principal) || safeEquals(this.authType, authType) || safeEquals(this.username, username) || safeEquals(this.password, password));

        this.principal = principal;
        this.authType = authType;
        this.username = username;
        this.password = password;
        this.canReauthenticate = HttpServletRequest.BASIC_AUTH.equals(authType) || HttpServletRequest.FORM_AUTH.equals(authType);
        return changed;
    }

    private boolean safeEquals(Object a, Object b) {
        return ((a == b) || (a != null && a.equals(b)) || (b != null && b.equals(a)));
    }
}
