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
