/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.WebMetaData;

import java.util.Iterator;
import java.util.List;

/**
 * Fix CapeDwarf Web usage - cleanup metadata.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfWebCleanupProcessor extends CapedwarfWebModificationDeploymentProcessor {

    private static final String WELD_LISTENER_PREFIX = "org.jboss.weld.environment.servlet.";
    private static final String EXPRESSION_FACTORY_KEY = "com.sun.faces.expressionFactory";

    @Override
    protected void doDeploy(DeploymentUnit unit, WebMetaData webMetaData, Type type) {
        if (type == Type.SPEC) {
            final List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
            removeContextParameters(contextParams);
            final List<ListenerMetaData> listeners = webMetaData.getListeners();
            removeListeners(listeners);
        }
    }

    protected void removeContextParameters(List<ParamValueMetaData> contextParams) {
        if (contextParams != null) {
            final Iterator<ParamValueMetaData> iter = contextParams.iterator();
            while (iter.hasNext()) {
                ParamValueMetaData pvmd = iter.next();
                if (pvmd.getParamName().equals(EXPRESSION_FACTORY_KEY))
                    iter.remove();
            }
        }
    }

    protected void removeListeners(List<ListenerMetaData> listeners) {
        if (listeners != null) {
            final Iterator<ListenerMetaData> iter = listeners.iterator();
            while (iter.hasNext()) {
                ListenerMetaData lmd = iter.next();
                if (lmd.getListenerClass().startsWith(WELD_LISTENER_PREFIX))
                    iter.remove();
            }
        }
    }
}
