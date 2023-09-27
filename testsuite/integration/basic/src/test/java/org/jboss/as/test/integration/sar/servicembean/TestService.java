/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.servicembean;

import javax.management.Attribute;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import org.jboss.system.ServiceMBeanSupport;

/**
 * An MBean that extends legacy {@link ServiceMBeanSupport}.
 *
 * @author Eduardo Martins
 */
public class TestService extends ServiceMBeanSupport implements TestServiceMBean {

    private static final String NAME = "java:global/env/foo/legacy";
    private static final String VALUE = "BAR";

    @Override
    protected void createService() throws Exception {
        getLog().trace("createService()");
        setTestResultMBeanAttribute("CreateServiceInvoked", true);
        server.addNotificationListener(new ObjectName("jboss:name=service-mbean-support-test"),
                new ObjectName("jboss:name=service-mbean-support-test-result"), null, new Object());
    }

    @Override
    protected void startService() throws Exception {
        getLog().trace("startService()");
        new InitialContext().bind(NAME, VALUE);
        setTestResultMBeanAttribute("StartServiceInvoked", true);
    }

    @Override
    protected void stopService() throws Exception {
        getLog().trace("stopService()");
        new InitialContext().unbind(NAME);
        setTestResultMBeanAttribute("StopServiceInvoked", true);
    }

    @Override
    protected void destroyService() throws Exception {
        getLog().trace("destroyService()");
        setTestResultMBeanAttribute("DestroyServiceInvoked", true);
    }

    private void setTestResultMBeanAttribute(String attributeName, boolean attributeValue) throws Exception {
        server.setAttribute(new ObjectName("jboss:name=service-mbean-support-test-result"), new Attribute(attributeName,
                attributeValue));
    }

}
