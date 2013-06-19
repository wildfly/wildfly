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
package org.wildfly.clustering.web.catalina.session;

import java.io.IOException;
import java.util.Random;

import org.apache.catalina.session.ManagerBase;
import org.wildfly.clustering.web.session.SessionIdentifierFactory;

/**
 * Creates session identifiers using the logic in {@link ManagerBase}.
 * @author Paul Ferraro
 */
public class CatalinaSessionIdentifierFactory extends ManagerBase implements SessionIdentifierFactory {

    private final Random random;

    public CatalinaSessionIdentifierFactory(Random random) {
        this.random = random;
    }

    @Override
    public String createSessionId() {
        return this.generateSessionId(this.random);
    }

    @Override
    public int getRejectedSessions() {
        return 0;
    }

    @Override
    public void setRejectedSessions(int rejectedSessions) {
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
    }

    @Override
    public void unload() throws IOException {
    }
}
