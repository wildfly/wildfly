/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2011, Red Hat, Inc., and individual contributors
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
 *
 */

package org.jboss.as.domain.management.security;

import java.io.IOError;
import java.util.IllegalFormatException;

/**
 * Describe the purpose
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class ConsoleMock implements ConsoleWrapper {

    private AssertConsoleBuilder responses;
    private int responseNo;

    public ConsoleMock() {
        this.responseNo = 0;
    }
    
    public void setResponses(AssertConsoleBuilder responses) {
        this.responses = responses;
    }


    @Override
    public Object format(String fmt, Object... args) throws IllegalFormatException {
        return responses.assertDisplayText(String.format(fmt,args));
    }

    @Override
    public void printf(String format, Object... args) throws IllegalFormatException {
        responses.assertDisplayText(String.format(format,args));
    }

    @Override
    public String readLine(String fmt, Object... args) throws IOError {
        responses.assertDisplayText(String.format(fmt,args));
        return responses.popAnswer();
    }

    @Override
    public char[] readPassword(String fmt, Object... args) throws IllegalFormatException, IOError {
        responses.assertDisplayText(String.format(fmt,args));
        return responses.popAnswer().toCharArray();
    }

    @Override
    public Object getConsole() {
        return null;
    }
}
