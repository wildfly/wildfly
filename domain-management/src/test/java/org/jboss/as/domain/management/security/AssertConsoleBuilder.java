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


package org.jboss.as.domain.management.security;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *  Assert builder for the console. Use this together with the ConsoleMock object
 *  to validate the expected messages are displayed and answers. You should always
 *  recorded in the same order as the user gets it present.
 *
 *  This example shows how you can record  a chain of messages and answers
 *
 *  AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
 *  expectedDisplayText("Please enter valid password").
 *  expectedDisplayText("\n").
 *  expectedInput("mysecretpassword");
 *
 *  ConsoleMock consoleMock = new ConsoleMock();
 *  consoleMock.setResponses(consoleBuilder);
 *
 *  ...
 *  ...
 *
 *  consoleBuilder.validate()
 *
*
* @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
*/
public class AssertConsoleBuilder {
    private enum Type {
        DISPLAY, INPUT
    }
    
    private class AssertConsole {
        private String text;
        private Type type;
    }

    private Queue<AssertConsole> queue = new LinkedList<AssertConsole>();

    /**
     * Recorded the expected display text
     * @param text - the display text
     * @return this
     */
    public AssertConsoleBuilder expectedDisplayText(String text) {
        AssertConsole assertConsole = new AssertConsole();
        assertConsole.text = text;
        assertConsole.type = Type.DISPLAY;
        queue.add(assertConsole);
        return this;
    }

    /**
     * Expected input string from the user
     * @param text
     * @return this
     */
    public AssertConsoleBuilder expectedInput(String text) {
        AssertConsole assertConsole = new AssertConsole();
        assertConsole.text = text;
        assertConsole.type = Type.INPUT;
        queue.add(assertConsole);
        return this;
    }

    /**
     * Assert the display text from the console, with the recorded display text.
     * if the doses't match it will fail
     * @param msg - display text from the console.
     * @return a String with  recorded display text
     */
    public String assertDisplayText(String msg) {
        AssertConsole assertConsole = queue.poll();

        if (assertConsole == null) {
            fail("Expected display text '"+msg+"' was not recorded");
        }

        if (!assertConsole.type.equals(Type.DISPLAY)) {
            fail("Wrong assert type, expect Type.DISPLAY");
        }
        assertEquals(assertConsole.text, msg);
        return assertConsole.text;
    }

    /**
     * Pop the next recorded answer to the console. if recorded is not
     * the Type.INPUT it will fail.
     * @return the recorded answer
     */
    public String popAnswer() {
        AssertConsole assertConsole = queue.poll();
        if (assertConsole == null) {
            fail("Expected answer was not recorded");
        }
        if (!assertConsole.type.equals(Type.INPUT)) {
            fail("Wrong assert type, expect Type.INPUT");
        }
        return assertConsole.text;
    }

    /**
     * validate if all recorded console assert has been asserted.
     * if not empty it will fail
     */
    public void validate() {
        StringBuffer notValidateAsserts = new StringBuffer();
        if (!queue.isEmpty()) {
            Iterator<AssertConsole> assertConsoleIter = queue.iterator();
            for (Iterator<AssertConsole> iterator = queue.iterator(); iterator.hasNext(); ) {
                AssertConsole assertConsole = iterator.next();
                notValidateAsserts.append("\"");
                notValidateAsserts.append(assertConsole.text);
                notValidateAsserts.append("\" ");
            }
        }
        assertTrue("There are still asserts in the queue that are not validated : "+notValidateAsserts.toString(),queue.isEmpty());
    }

}
