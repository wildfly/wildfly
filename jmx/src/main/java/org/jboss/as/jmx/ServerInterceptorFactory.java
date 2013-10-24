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
package org.jboss.as.jmx;

import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.jboss.as.controller.AccessAuditContext;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.security.UserInfo;
import org.jboss.remotingjmx.ServerMessageInterceptor;
import org.jboss.remotingjmx.ServerMessageInterceptorFactory;

/**
 * A {@link ServerMessageInterceptorFactory} responsible for supplying a {@link ServerMessageInterceptor} for associating the
 * Subject of the remote user with the current request.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class ServerInterceptorFactory implements ServerMessageInterceptorFactory {

    @Override
    public ServerMessageInterceptor create(Channel channel) {
        return new Interceptor(channel);
    }

    private static class Interceptor implements ServerMessageInterceptor {

        private final Channel channel;

        private Interceptor(final Channel channel) {
            this.channel = channel;
        }

        @Override
        public void handleEvent(final Event event) throws IOException {
            UserInfo userInfo = channel.getConnection().getUserInfo();
            final Subject subject;
            if (userInfo instanceof SubjectUserInfo) {
                subject = ((SubjectUserInfo) userInfo).getSubject();
            } else {
                subject = new Subject();
            }

            try {
                AccessAuditContext.doAs(subject, new PrivilegedExceptionAction<Void>() {

                    @Override
                    public Void run() throws IOException {
                        SecurityActions.currentAccessAuditContext().setAccessMechanism(AccessMechanism.JMX);
                        event.run();

                        return null;
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception cause = e.getException();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw new IOException(cause);
                }
            }

        }

    }

}
