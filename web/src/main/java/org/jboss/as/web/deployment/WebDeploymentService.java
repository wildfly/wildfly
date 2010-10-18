/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.web.deployment;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service starting a web deployment.
 *
 * @author Emanuel Muckenhuber
 */
class WebDeploymentService implements Service<Context> {

    private static final Logger log = Logger.getLogger("org.jboss.web.deployment");
    private final StandardContext context;

    public WebDeploymentService(final StandardContext context) {
        this.context = context;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext startContext) throws StartException {
        final long time = System.currentTimeMillis();
        try {
            context.create();
        } catch (Exception e) {
            throw new StartException("failed to create context", e);
        }
        try {
            context.start();
        } catch (LifecycleException e) {
            throw new StartException("failed to start context", e);
        }
        Logger.getLogger("org.jboss.web").info("starting context " + (System.currentTimeMillis() - time));
        /*
         * Add security association valve after the authorization valves so that the authenticated user may be associated
         * with the request thread/session.
         */
        /* TODO
        if (!config.isStandalone())
        {
           SecurityAssociationValve securityAssociationValve = new SecurityAssociationValve(metaData, config.getSecurityManagerService());
           securityAssociationValve.setSubjectAttributeName(config.getSubjectAttributeName());
           context.addValve(securityAssociationValve);
        }*/
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext stopContext) {
        try {
            context.stop();
        } catch (LifecycleException e) {
            log.error("exception while stopping context", e);
        }
        try {
            context.destroy();
        } catch (Exception e) {
            log.error("exception while destroying context", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized Context getValue() throws IllegalStateException {
        final Context context = this.context;
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }

}
