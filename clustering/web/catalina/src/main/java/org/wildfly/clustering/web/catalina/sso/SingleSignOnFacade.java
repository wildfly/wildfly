/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.catalina.sso;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.authenticator.SingleSignOnEntry;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.sso.WebApplication;
import org.wildfly.clustering.web.sso.AuthenticationType;
import org.wildfly.clustering.web.sso.Credentials;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.SSOManager;

/**
 * {@link SingleSignOn} facade for an {@link SSOManager}
 * @author Paul Ferraro
 */
public class SingleSignOnFacade extends SingleSignOn implements ManagerRegistry, LifecycleListener {

    private final ConcurrentMap<WebApplication, Manager> managers = new ConcurrentHashMap<>();
    private final SSOManager<LocalSSOContext> manager;

    public SingleSignOnFacade(SSOManager<LocalSSOContext> manager) {
        this.manager = manager;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Batcher batcher = this.manager.getBatcher();
        boolean started = batcher.startBatch();
        boolean successful = false;
        try {
            super.invoke(request, response);
            successful = true;
        } finally {
            if (started) {
                batcher.endBatch(successful);
            }
        }
    }

    @Override
    public Manager getManager(WebApplication application) {
        return this.managers.get(application);
    }

    @Override
    public WebApplication getApplication(Manager manager) {
        Context context = (Context) manager.getContainer();
        Host host = (Host) context.getParent();
        return new WebApplication(context.getPath(), host.getName());
    }

    private WebApplication getApplication(Session session) {
        return this.getApplication(session.getManager());
    }

    @Override
    protected void deregister(String ssoId, Session session) {
        this.removeSession(ssoId, session);
    }

    @Override
    protected SingleSignOnEntry lookup(String ssoId) {
        SSO<LocalSSOContext> sso = this.manager.findSSO(ssoId);
        return (sso != null) ? new SingleSignOnEntryFacade(sso, this) : null;
    }

    @Override
    protected void removeSession(String ssoId, Session session) {
        SSO<LocalSSOContext> sso = this.manager.findSSO(ssoId);
        if (sso != null) {
            sso.getSessions().removeSession(this.getApplication(session));
            if (sso.getSessions().getApplications().isEmpty()) {
                sso.invalidate();
            }
        }
    }

    @Override
    public void associate(String ssoId, Session session) {
        SSO<LocalSSOContext> sso = this.manager.findSSO(ssoId);
        Manager manager = session.getManager();
        WebApplication application = this.getApplication(manager);
        if (sso != null) {
            sso.getSessions().addSession(application, session.getId());
        }
        if (this.managers.putIfAbsent(this.getApplication(session), session.getManager()) == null) {
            ((Lifecycle) manager).addLifecycleListener(this);
        }
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        String type = event.getType();
        if (Lifecycle.STOP_EVENT.equals(type)) {
            Lifecycle source = event.getLifecycle();
            Manager manager = (Manager) source;
            if (this.managers.remove(this.getApplication(manager)) != null) {
                source.removeLifecycleListener(this);
            }
        }
    }

    @Override
    public void deregister(String ssoId) {
        SSO<LocalSSOContext> sso = this.manager.findSSO(ssoId);
        if (sso != null) {
            sso.invalidate();
        }
    }

    @Override
    public void register(String ssoId, Principal principal, String authType, String username, String password) {
        SSO<LocalSSOContext> sso = this.manager.createSSO(ssoId);
        sso.getLocalContext().setPrincipal(principal);
        Credentials credentials = sso.getCredentials();
        credentials.setAuthenticationType(AuthenticationType.valueOf(authType));
        credentials.setUser(username);
        credentials.setPassword(password);
    }

    @Override
    public void update(String ssoId, Principal principal, String authType, String username, String password) {
        SSO<LocalSSOContext> sso = this.manager.findSSO(ssoId);
        if (sso != null) {
            sso.getLocalContext().setPrincipal(principal);
            Credentials credentials = sso.getCredentials();
            credentials.setAuthenticationType(AuthenticationType.valueOf(authType));
            credentials.setUser(username);
            credentials.setPassword(password);
        }
    }
}
