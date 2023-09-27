/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.system;

/**
 * An interface describing a JBoss service MBean.
 *
 * @see Service
 * @see ServiceMBeanSupport
 *
 * @author <a href="mailto:rickard.oberg@telkel.com">Rickard Oberg</a>
 * @author <a href="mailto:andreas@jboss.org">Andreas Schaefer</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81033 $
 */
public interface ServiceMBean extends Service {
    // Constants -----------------------------------------------------

    /** ServiceController notification types corresponding to service lifecycle events */
    String CREATE_EVENT = "org.jboss.system.ServiceMBean.create";
    String START_EVENT = "org.jboss.system.ServiceMBean.start";
    String STOP_EVENT = "org.jboss.system.ServiceMBean.stop";
    String DESTROY_EVENT = "org.jboss.system.ServiceMBean.destroy";

    String[] states = { "Stopped", "Stopping", "Starting", "Started", "Failed", "Destroyed", "Created",
            "Unregistered", "Registered" };

    /** The Service.stop has completed */
    int STOPPED = 0;
    /** The Service.stop has been invoked */
    int STOPPING = 1;
    /** The Service.start has been invoked */
    int STARTING = 2;
    /** The Service.start has completed */
    int STARTED = 3;
    /** There has been an error during some operation */
    int FAILED = 4;
    /** The Service.destroy has completed */
    int DESTROYED = 5;
    /** The Service.create has completed */
    int CREATED = 6;
    /** The MBean has been created but has not completed MBeanRegistration.postRegister */
    int UNREGISTERED = 7;
    /** The MBean has been created and has completed MBeanRegistration.postRegister */
    int REGISTERED = 8;

    // Public --------------------------------------------------------

    String getName();

    int getState();

    String getStateString();

    /** Detyped lifecycle invocation */
    void jbossInternalLifecycle(String method) throws Exception;
}
