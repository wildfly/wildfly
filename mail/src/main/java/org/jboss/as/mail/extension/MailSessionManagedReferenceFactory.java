/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.mail.extension;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.value.ImmediateValue;

/**
* @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
*/
class MailSessionManagedReferenceFactory implements ContextListAndJndiViewManagedReferenceFactory {

    private final MailSessionService service;

    public MailSessionManagedReferenceFactory(MailSessionService service) {
        this.service = service;
    }

    @Override
    public String getJndiViewInstanceValue() {
        return String.valueOf(getReference().getInstance());
    }

    @Override
    public String getInstanceClassName() {
        final Object value = getReference().getInstance();
        return value != null ? value.getClass().getName() : ContextListManagedReferenceFactory.DEFAULT_INSTANCE_CLASS_NAME;
    }

    @Override
    public ManagedReference getReference() {
        return new ValueManagedReference(new ImmediateValue<Object>(service.getValue()));
    }
}
