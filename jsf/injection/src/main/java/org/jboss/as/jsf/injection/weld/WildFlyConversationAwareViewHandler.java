/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.jsf.injection.weld;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.jboss.as.jsf.logging.JSFLogger;
import org.jboss.as.jsf.deployment.JSFDependencyProcessor;
import org.jboss.weld.module.jsf.ConversationAwareViewHandler;

/**
 * If this is a CDI-enabled app, then delegate to a wrapped Weld ViewHandler.
 * Otherwise, delegate to the parent ViewHandler in the chain.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2013 Red Hat Inc.
 */
public class WildFlyConversationAwareViewHandler extends ViewHandlerWrapper {

    private ViewHandler wrapped;

    /**
     * This method will never be called by the JSF implementation.  The JSF impl recognizes the single-arg
     * constructor.  The only reason the no-arg constructor is here is because the TCK creates the ViewHandler outside of
     * the JSF impl.  So we need it to pass the test.
     */
    public WildFlyConversationAwareViewHandler() {
        super();

        JSFLogger.ROOT_LOGGER.viewHandlerImproperlyInitialized();
    }

    public WildFlyConversationAwareViewHandler(ViewHandler parent) {
        super();

        if (isCDIApp()) {
            wrapped = new ConversationAwareViewHandler(parent);
        } else {
            wrapped = parent;
        }
    }

    private static boolean isCDIApp() {
        ServletContext ctx = (ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext();
        return Boolean.parseBoolean(ctx.getInitParameter(JSFDependencyProcessor.IS_CDI_PARAM));
    }

    @Override
    public ViewHandler getWrapped() {
        return wrapped;
    }

}
