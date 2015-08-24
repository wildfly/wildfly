/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.services.workmanager.transport;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.PrivilegedAction;

import org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.RpcDispatcher.Marshaller;
import org.jgroups.util.Buffer;
import org.jgroups.util.Util;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link JGroupsTransport} capable of handling unknown fork responses.
 * @author Paul Ferraro
 */
public class ForkChannelTransport extends JGroupsTransport implements Marshaller {

    private final ChannelFactory factory;

    public ForkChannelTransport(ChannelFactory factory) {
        this.factory = factory;
    }

    @Override
    public void startup() throws Throwable {
        super.startup();

        // TODO Modify ironjacamar to expose the RpcDispatcher thereby removing the need for reflection
        PrivilegedAction<RpcDispatcher> action = new PrivilegedAction<RpcDispatcher>() {
            @Override
            public RpcDispatcher run() {
                try {
                    Field field = JGroupsTransport.class.getDeclaredField("disp");
                    boolean accessible = field.isAccessible();
                    if (!accessible) {
                        field.setAccessible(true);
                    }
                    try {
                        return (RpcDispatcher) field.get(this);
                    } finally {
                        if (!accessible) {
                            field.setAccessible(false);
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        RpcDispatcher dispatcher = WildFlySecurityManager.doUnchecked(action);
        dispatcher.setResponseMarshaller(this);
    }

    @Override
    public Buffer objectToBuffer(Object obj) throws Exception {
        return new Buffer(Util.objectToByteBuffer(obj));
    }

    @Override
    public Object objectFromBuffer(byte[] buffer, int offset, int length) throws Exception {
        return this.factory.isUnknownForkResponse(ByteBuffer.wrap(buffer, offset, length)) ? null : Util.objectFromByteBuffer(buffer, offset, length);
    }
}
