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

package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;

/**
 * Sets up the component description for a MDB with the pool name configured via {@link org.jboss.ejb3.annotation.Pool}
 * annotation and/or the deployment descriptor
 *
 * @author Jaikiran Pai
 */
public class MessageDrivenBeanPoolMergingProcessor extends AbstractPoolMergingProcessor<MessageDrivenComponentDescription> {

    public MessageDrivenBeanPoolMergingProcessor() {
        super(MessageDrivenComponentDescription.class);
    }

    @Override
    protected void setPoolName(final MessageDrivenComponentDescription componentDescription, final String poolName) {
        componentDescription.setPoolConfigName(poolName);
    }
}
