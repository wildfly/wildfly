/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.jbossts.XTSService;
import org.jboss.jbossts.xts.environment.WSCEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.StartException;

/**
 * Main XTS service
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
public class XTSManagerService extends AbstractService<XTSService> {
    private final String coordinatorURL;
    private volatile org.jboss.jbossts.XTSService xtsService;

    public XTSManagerService(String coordinatorURL) {
        this.coordinatorURL = coordinatorURL;
        this.xtsService = null;
    }

    @Override
    public XTSService getValue() throws IllegalStateException {
        return xtsService;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        // XTS expects the TCCL to be set to something that will locate the XTS service implementation classes.
        final ClassLoader loader = XTSService.class.getClassLoader();
        SecurityActions.setContextLoader(loader);
        try {
            if (coordinatorURL !=null ) {
                WSCEnvironmentBean wscEnVBean = XTSPropertyManager.getWSCEnvironmentBean();
                wscEnVBean.setCoordinatorURL11(coordinatorURL);
            }
            XTSService service = new XTSService();
            try {
                service.start();
            } catch (Exception e) {
                throw new StartException("XTS service start failed", e);
            }
            xtsService = service;
        } finally {
            SecurityActions.setContextLoader(null);
        }
    }

    public synchronized void stop(final StopContext context) {
        if (xtsService != null) {
            try {
                xtsService.stop();
            } catch (Exception e) {
                // ignore?
            }
        }
    }
}
