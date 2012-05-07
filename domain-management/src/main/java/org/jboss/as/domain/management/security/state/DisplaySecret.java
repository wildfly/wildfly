/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security.state;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.security.AddPropertiesUser.NEW_LINE;

import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.util.Base64;

/**
 * A state to display the secret element needed for server to server password defintion.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DisplaySecret implements State {

    private final StateValues stateValues;
    private final ConsoleWrapper theConsole;

    public DisplaySecret(ConsoleWrapper theConsole, final StateValues stateValues) {
        this.stateValues = stateValues;
        this.theConsole = theConsole;
    }

    public State execute() {
        String pwdBase64 = Base64.encodeBytes(new String(stateValues.getPassword()).getBytes());
        theConsole.printf(MESSAGES.secretElement(pwdBase64));
        theConsole.printf(NEW_LINE);

        // This is now the final state so return null.
        return null;
    }

}
