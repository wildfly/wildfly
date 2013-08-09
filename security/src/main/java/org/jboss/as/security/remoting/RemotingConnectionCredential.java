/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.security.remoting;

import javax.security.auth.Subject;

import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.security.UserInfo;

/**
 * A Credential wrapping a Remoting {@link Connection}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class RemotingConnectionCredential {

    private final Connection connection;
    private final Subject subject;

    public RemotingConnectionCredential(final Connection connection) {
        this.connection = connection;
        Subject subject = null;
        UserInfo userInfo = connection.getUserInfo();
        if (userInfo instanceof SubjectUserInfo) {
            subject = ((SubjectUserInfo) userInfo).getSubject();
        }
        this.subject = subject;
    }

    Connection getConnection() {
        return connection;
    }

    public Subject getSubject() {
        return subject;
    }

    @Override
    public int hashCode() {
        return connection.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RemotingConnectionCredential ? equals((RemotingConnectionCredential) obj) : false;
    }

    public boolean equals(RemotingConnectionCredential obj) {
        return connection.equals(obj.connection);
    }
}
