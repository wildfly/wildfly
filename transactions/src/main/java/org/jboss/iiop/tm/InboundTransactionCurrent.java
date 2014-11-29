/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.iiop.tm;

import javax.transaction.Transaction;

import org.omg.CORBA.Current;

/**
 * Interface to be implemented by a CORBA OTS provider for integration with
 * JBossAS. The CORBA OTS provider must (i) create an object that implements
 * this interface and (ii) register an initial reference for that object
 * with the JBossAS ORB, under name "InboundTransactionCurrent".
 * <p/>
 * Step (ii) above should be done by a call
 * <code>orbInitInfo.register_initial_reference</code> within the
 * <code>pre_init</code> method of an
 * <code>org.omg.PortableInterceptor.ORBInitializer</code>,
 * which will probably be also the initializer that registers a server request
 * interceptor for the OTS provider.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 * @version $Revision$
 */
public interface InboundTransactionCurrent extends Current {
    String NAME = "InboundTransactionCurrent";

    /**
     * Gets the Transaction instance associated with the current incoming
     * request. This method should be called only by code that handles incoming
     * requests; its return value is undefined in the case of a call issued
     * outside of a request scope.
     *
     * @return the javax.transaction.Transaction instance associated with the
     *         current incoming request, or null if that request was not issued
     *         within the scope of some transaction.
     */
    Transaction getCurrentTransaction();

}
