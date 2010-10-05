/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.model;

/**
 * Server group properties update.
 *
 * @author Emanuel Muckenhuber
 */
public class ServerGroupPropertiesUpdate extends AbstractDomainModelUpdate<Void> {

    private static final long serialVersionUID = -466918965014839469L;

    private final String groupName;
    private final AbstractPropertyUpdate update;

    public ServerGroupPropertiesUpdate(String groupName, AbstractPropertyUpdate update) {
        this.groupName = groupName;
        this.update = update;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(DomainModel element) throws UpdateFailedException {
        final ServerGroupElement group = element.getServerGroup(groupName);
        if(group == null) {
            throw new UpdateFailedException(String.format("Server group (%s) does not exist", groupName));
        }
        update.applyUpdate(group.getSystemProperties());
    }

    /** {@inheritDoc} */
    public AbstractDomainModelUpdate<?> getCompensatingUpdate(DomainModel element) {
        final ServerGroupElement group = element.getServerGroup(groupName);
        final PropertiesElement original = group.getSystemProperties();
        return new ServerGroupPropertiesUpdate(groupName, update.getCompensatingUpdate(original));
    }

    /** {@inheritDoc} */
    protected AbstractServerModelUpdate<Void> getServerModelUpdate() {
        return new ServerSystemPropertyUpdate(update);
    }

}
