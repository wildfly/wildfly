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
package org.jboss.as.test.integration.ee.injection.support.servlet;

import javax.inject.Inject;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructBinding;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;
import org.jboss.as.test.integration.ee.injection.support.ProducedString;

@AroundConstructBinding
@ComponentInterceptorBinding
@WebListener
public class TestListener implements ServletRequestListener {

    @Inject
    private Alpha alpha;

    private Bravo bravo;

    private String name;

    @Inject
    public TestListener(@ProducedString String name) {
        this.name = name + "#TestListener";
    }

    @Inject
    public void setBravo(Bravo bravo) {
        this.bravo = bravo;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        HttpServletRequest req = (HttpServletRequest) sre.getServletRequest();
        req.setAttribute("field.injected", alpha != null);
        req.setAttribute("setter.injected", bravo != null);
        req.setAttribute("name", name);
    }

}
