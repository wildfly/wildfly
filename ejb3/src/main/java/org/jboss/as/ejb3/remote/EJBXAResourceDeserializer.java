/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import java.io.IOException;
import java.io.ObjectInputStream;

import javax.transaction.xa.XAResource;

import org.jboss.ejb.client.EJBClientManagedTransactionContext;

import com.arjuna.ats.jta.recovery.SerializableXAResourceDeserializer;

/**
 * A {@link SerializableXAResourceDeserializer} responsible for deserializing EJB XAResource(s)
 *
 * @author Jaikiran Pai
 */
class EJBXAResourceDeserializer implements SerializableXAResourceDeserializer {

    static final EJBXAResourceDeserializer INSTANCE = new EJBXAResourceDeserializer();

    private EJBXAResourceDeserializer() {

    }

    @Override
    public boolean canDeserialze(final String className) {
        return EJBClientManagedTransactionContext.isEJBXAResourceClass(className);
    }

    @Override
    public XAResource deserialze(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        // just calling readObject is fine (see ObjectInputStream#latestUserDefinedLoader() for details on why we have to invoke this
        // from within an EJB module class)
        return (XAResource) objectInputStream.readObject();
    }

}
