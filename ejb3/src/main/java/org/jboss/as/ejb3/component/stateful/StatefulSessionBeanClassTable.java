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

package org.jboss.as.ejb3.component.stateful;

import java.io.Serializable;
import java.util.UUID;

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

import org.jboss.as.ee.component.BasicComponentInstance;
import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.component.session.SessionBeanComponentInstance;
import org.jboss.as.naming.ImmediateManagedReference;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.jboss.SimpleClassTable;

/**
 * @author Paul Ferraro
 */
public class StatefulSessionBeanClassTable extends SimpleClassTable {

    private static Class<?>[] classes = new Class<?>[] {
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
        ImmediateManagedReference.class,
    };

    public StatefulSessionBeanClassTable() {
        super(classes);
    }
}
