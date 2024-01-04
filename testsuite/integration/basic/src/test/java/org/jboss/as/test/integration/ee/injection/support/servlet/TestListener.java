/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;

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
