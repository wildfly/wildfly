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

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;


@Stateless
@Remote(IBeanLifecycleCallback.class)
@SecurityDomain("ejb3-tests")
public class SLSBLifecycleCallback implements IBeanLifecycleCallback {

    private static Logger log = Logger.getLogger(SLSBLifecycleCallback.class);

    @Resource
    private SessionContext sessContext;

    private ITestResultsSingleton getSingleton() {
        return (ITestResultsSingleton) sessContext.lookup("java:global/single/" + TestResultsSingleton.class.getSimpleName());
    }

    public void remove() {
        // nothing to do
    }

    @PostConstruct
    public void init() throws Exception {
        // on Stateless bean is not permitted to call getCallerPrincipal on @PostConstruct
        ITestResultsSingleton results = getSingleton();
        log.trace(SLSBLifecycleCallback.class.getSimpleName() + " @PostConstruct called");
        Principal princ = null;
        try {
            princ = sessContext.getCallerPrincipal();
        } catch (IllegalStateException e) {
            results.setSlsb("postconstruct", "OKstart");
            return;
        }
        results.setSlsb("postconstruct", "Method getCallerPrincipal was called from @PostConstruct with result: " + princ);
    }

    @PreDestroy
    public void tearDown() throws Exception {
        // on Stateless bean is not permitted to call getCallerPrincipal on @PreDestroy
        ITestResultsSingleton results = getSingleton();
        log.trace(SLSBLifecycleCallback.class.getSimpleName() + " @PreDestroy called");
        Principal princ = null;
        try {
            princ = sessContext.getCallerPrincipal();
        } catch (IllegalStateException e) {
            results.setSlsb("predestroy", "OKstop");
            return;
        }
        results.setSlsb("predestroy", "Method getCallerPrincipal was called from @PreDestroy with result: " + princ);
    }

    public String get() {
        return "stateless";
    }
}
