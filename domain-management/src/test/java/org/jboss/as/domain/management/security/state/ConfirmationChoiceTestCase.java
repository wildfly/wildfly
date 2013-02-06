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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.security.AssertConsoleBuilder;
import org.jboss.msc.service.StartException;
import org.junit.Test;

/**
 * Test the confirmation state
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class ConfirmationChoiceTestCase extends PropertyTestHelper {

    public static final String USER_DISPLAY_TEXT = "User display text";
    public static final String PLEASE_ANSWER = "Please answer";
/*
    @Test
    public void testState() throws IOException, StartException {

        ErrorState errorState = new ErrorState(consoleMock,null);
        PromptPasswordState passwordState = new PromptPasswordState(consoleMock,null);

        ConfirmationChoice confirmationChoice = new ConfirmationChoice(consoleMock, USER_DISPLAY_TEXT, PLEASE_ANSWER, passwordState,errorState);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                expectedConfirmMessage(USER_DISPLAY_TEXT, PLEASE_ANSWER, "y");

        consoleMock.setResponses(consoleBuilder);
        State promptPasswordState = confirmationChoice.execute();

        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testWrongAnswer() throws IOException, StartException {

        ErrorState errorState = new ErrorState(consoleMock,null);
        PromptPasswordState passwordState = new PromptPasswordState(consoleMock,null);

        ConfirmationChoice confirmationChoice = new ConfirmationChoice(consoleMock, USER_DISPLAY_TEXT, PLEASE_ANSWER, passwordState,errorState);

        List<String> acceptedValues = new ArrayList<String>(4);
        acceptedValues.add(MESSAGES.yes());
        if (MESSAGES.shortYes().length() > 0) {
            acceptedValues.add(MESSAGES.shortYes());
        }
        acceptedValues.add(MESSAGES.no());
        if (MESSAGES.shortNo().length() > 0) {
            acceptedValues.add(MESSAGES.shortNo());
        }
        StringBuilder sb = new StringBuilder(acceptedValues.get(0));
        for (int i = 1; i < acceptedValues.size() - 1; i++) {
            sb.append(", ");
            sb.append(acceptedValues.get(i));
        }

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedConfirmMessage(USER_DISPLAY_TEXT,
                PLEASE_ANSWER, "d").expectedErrorMessage(
                MESSAGES.invalidConfirmationResponse(sb.toString(), acceptedValues.get(acceptedValues.size() - 1)));

        consoleMock.setResponses(consoleBuilder);
        State nextState = confirmationChoice.execute();

        assertTrue("Expected the next state to be ErrorState", nextState instanceof ErrorState);
        nextState.execute();
        consoleBuilder.validate();
    }

    */
}
