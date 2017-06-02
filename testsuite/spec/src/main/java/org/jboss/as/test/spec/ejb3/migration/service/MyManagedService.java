/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.spec.ejb3.migration.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Previous version of JBoss EJB 3 extensions allowed for JMX bound Service EJBs.
 *
 * The Service EJB has been pruned, because now the specification contains Singleton EJBs.
 *
 * The Management interface has been pruned, because the lifecycle methods are
 * a double edged sword. (See EJBTHREE-655)
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author Adam Bien
 */
// @Service // no longer available
@Singleton // the replacement
// @Management // no longer available
@Startup
public class MyManagedService implements MyManagedServiceMXBean {
    private MBeanServer mBeanServer;
    private ObjectName objectName;

    @Resource(lookup = "java:app/AppName")
    private String appName;

    @Resource(lookup = "java:module/ModuleName")
    private String moduleName;

    @Resource
    private SessionContext ctx;

    @Override
    public String ping() {
        return "pong";
    }

    @PostConstruct
    protected void postConstruct() {
        try {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
            objectName = new ObjectName("MyManagedService:app=" + appName + ",module=" + moduleName);
            mBeanServer.registerMBean(ctx.getBusinessObject(MyManagedServiceMXBean.class), objectName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
        // call the old Service start method
        start();
    }

    @PreDestroy
    protected void preDestroy() {
        // call the old Service stop method
        stop();
        try {
            mBeanServer.unregisterMBean(objectName);
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    // Think carefully whether to make this protected or public.
    // In essence it'll never be capable of really influencing the life-cycle of the Singleton.
    protected void start() {
        System.out.println(objectName + " is started");
    }

    protected void stop() {
        System.out.println(objectName + " is stopped");
    }
}
