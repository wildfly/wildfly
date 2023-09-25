/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

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
