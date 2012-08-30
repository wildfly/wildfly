/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.web.deployment;

import org.apache.jasper.runtime.JspApplicationContextImpl;
import org.jboss.as.web.common.ExpressionFactoryWrapper;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import java.util.List;

/**
 * Listener that allows the expression factory to be wrapped
 *
 * @author Stuart Douglas
 */
public class JspInitializationListener implements ServletRequestListener {

    private volatile boolean installed = false;

    public static final String CONTEXT_KEY = "org.jboss.as.web.deployment.JspInitializationListener.wrappers";

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        if (!installed) {
            synchronized (this) {
                if (!installed) {
                    installed = true;
                    List<ExpressionFactoryWrapper> wrapper = (List<ExpressionFactoryWrapper>) sre.getServletContext().getAttribute(CONTEXT_KEY);
                    sre.getServletContext().setAttribute(JspApplicationContextImpl.class.getName(), new JspApplicationContextWrapper(JspApplicationContextImpl.getInstance(sre.getServletContext()), wrapper, sre.getServletContext()));
                }
            }
        }
    }
}
