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

package org.jboss.as.test.integration.ejb.stateful.exception;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;

/**
 * stateful session bean
 */
@Stateful
@Remote
@LocalBean
public class SFSB1 implements SFSB1Interface {

    public static final String MESSAGE = "Expected Exception";

    @EJB
    private DestroyMarkerBean marker;


    @PostConstruct
    public void postConstruct() {
        marker.set(false);
    }

    @PreDestroy
    public void preDestroy() {
        marker.set(true);
    }

    public void systemException() {
        throw new RuntimeException(MESSAGE);
    }

    public void ejbException() {
        throw new EJBException(MESSAGE);
    }

    public void userException() throws TestException {
        throw new TestException(MESSAGE);
    }

    @Remove
    public void remove() {
        // just removed
    }
}
