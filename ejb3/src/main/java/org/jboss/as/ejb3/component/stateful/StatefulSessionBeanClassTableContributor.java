/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.ejb.client.SessionID;
import org.jboss.marshalling.ClassTable;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.jboss.IdentityClassTable;

import jakarta.ejb.EJBHome;
import jakarta.ejb.EJBMetaData;
import jakarta.ejb.EJBObject;
import jakarta.ejb.Handle;
import jakarta.ejb.HomeHandle;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.UserTransaction;

/**
 * Contributes to the JBoss Marshalling class table used when marshalling stateful session bean instances.
 * @author Paul Ferraro
 */
@MetaInfServices(ClassTable.class)
public class StatefulSessionBeanClassTableContributor extends IdentityClassTable {

    public StatefulSessionBeanClassTableContributor() {
        super(List.of(
                SessionContext.class,
                UserTransaction.class,
                EntityManager.class,
                EntityManagerFactory.class,
                Timer.class,
                SessionID.class,
                SessionID.Serialized.class,
                EJBHome.class,
                EJBObject.class,
                Handle.class,
                HomeHandle.class,
                EJBMetaData.class,
                UUID.class,
                StatefulSessionComponentInstance.class,
                SessionBeanComponentInstance.class,
                EjbComponentInstance.class,
                BasicComponentInstance.class,
                Serializable.class,
                StatefulSerializedProxy.class,
                ManagedReference.class,
                ValueManagedReferenceFactory.ValueManagedReference.class,
                SerializedCdiInterceptorsKey.class,
                SerializedStatefulSessionComponent.class,
                ImmediateManagedReference.class));
    }
}
