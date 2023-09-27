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
import jakarta.ejb.Remove;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;


@Stateful
@Remote(IBeanLifecycleCallback.class)
@SecurityDomain("ejb3-tests")
public class SFSBLifecycleCallback implements IBeanLifecycleCallback {

    private static Logger log = Logger.getLogger(SFSBLifecycleCallback.class);

    @Resource
    private SessionContext sessContext;

    private ITestResultsSingleton getSingleton() {
        return (ITestResultsSingleton) sessContext.lookup("java:global/single/" + TestResultsSingleton.class.getSimpleName());
    }

    @Remove
    public void remove() {
        // removing the sfsb
    }

    @PostConstruct
    public void init() throws Exception {
        // on Stateful bean is permitted to call getCallerPrincipal on @PostConstruct
        ITestResultsSingleton results = this.getSingleton();

        Principal princ = sessContext.getCallerPrincipal();
        results.setSfsb("postconstruct", princ.getName() + "start");
        log.trace(SFSBLifecycleCallback.class.getSimpleName() + " @PostConstruct called");
    }

    @PreDestroy
    public void tearDown() throws Exception {
        // on Stateful bean is permitted to call getCallerPrincipal on @PreDestroy
        ITestResultsSingleton results = this.getSingleton();

        Principal princ = sessContext.getCallerPrincipal();
        results.setSfsb("predestroy", princ.getName() + "stop");
        log.trace(SFSBLifecycleCallback.class.getSimpleName() + " @PreDestroy called");
    }

    public String get() {
        log.trace("stateful get() principal: " + sessContext.getCallerPrincipal());
        return "stateful";
    }
}
