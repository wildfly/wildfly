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

import org.jboss.as.domain.management.security.AssertConsoleBuilder;
import org.jboss.msc.service.StartException;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.junit.Assert.assertTrue;

/**
 * Test the password weakness
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class WeakCheckStateTestCase extends PropertyTestHelper {

    @Test
    public void testState() throws IOException, StartException {

        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder();
        consoleMock.setResponses(consoleBuilder);

        State duplicateUserCheckState = weakCheckState.execute();

        assertTrue("Expected the next state to be DuplicateUserCheckState", duplicateUserCheckState instanceof DuplicateUserCheckState);
        consoleBuilder.validate();
    }

    @Test
    public void testWrongPassword() {
        values.setUserName("thesame");
        values.setPassword("thesame".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.usernamePasswordMatch());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }
    
    @Test
    public void testForbiddenPassword() {
        values.setUserName("willFail");
        values.setPassword("administrator".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustNotBeEqual("administrator"));
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }
    
    @Test
    public void testWeakPassword() {
        values.setUserName("willFail");
        values.setPassword("zxcvbnm1@".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordNotStrongEnough("MODERATE", "MEDIUM"));
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }
    
    @Test
    public void testTooShortPassword() {
        values.setUserName("willFail");
        values.setPassword("1QwD%rf".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordNotLontEnough(8));
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }
    
    @Test
    public void testNoDigitInPassword() {
        values.setUserName("willFail");
        values.setPassword("!QwD%rGf".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustHaveDigit());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }
    
    @Test
    public void testNoSymbolInPassword() {
        values.setUserName("willFail");
        values.setPassword("1QwD5rGf".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustHaveSymbol());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }
    
    @Test
    public void testNoAlphaInPassword() {
        values.setUserName("willFail");
        values.setPassword("1$*>5&#}".toCharArray());
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustHaveAlpha());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }

    @Test
    public void testUsernameNotAlphaNumeric() {
        values.setUserName("username&");
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.usernameNotAlphaNumeric());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptNewUserState = errorState.execute();
        assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
        consoleBuilder.validate();
    }

    @Test
    public void testBadUsername() {
        String[] BAD_USER_NAMES = {"admin", "administrator", "root"};
        for (String userName : BAD_USER_NAMES) {

            values.setUserName(userName);
            WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

            AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().
                    expectedConfirmMessage(MESSAGES.usernameEasyToGuess(userName), MESSAGES.sureToAddUser(userName), "n");
            consoleMock.setResponses(consoleBuilder);

            State confirmationChoice = weakCheckState.execute();

            assertTrue("Expected the next state to be ConfirmationChoice", confirmationChoice instanceof ConfirmationChoice);
            State promptNewUserState = confirmationChoice.execute();
            assertTrue("Expected the next state to be PromptNewUserState", promptNewUserState instanceof PromptNewUserState);
            consoleBuilder.validate();
        }
    }

    @Test
    public void testUsernameWithValidPunctuation() {
        values.setUserName("username.@\\=,/");
        WeakCheckState weakCheckState = new WeakCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder();
        consoleMock.setResponses(consoleBuilder);

        State duplicateUserCheckState = weakCheckState.execute();

        assertTrue("Expected the next state to be DuplicateUserCheckState", duplicateUserCheckState instanceof DuplicateUserCheckState);
        consoleBuilder.validate();
    }

}
