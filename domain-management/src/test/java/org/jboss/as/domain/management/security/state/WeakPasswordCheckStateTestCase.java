package org.jboss.as.domain.management.security.state;

import org.jboss.as.domain.management.security.AssertConsoleBuilder;
import org.junit.Test;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.junit.Assert.assertTrue;

/**
 * Test the password weakness
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class WeakPasswordCheckStateTestCase extends PropertyTestHelper {

    @Test
    public void testWrongPassword() {
        values.setUserName("thesame");
        values.setPassword("thesame".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.usernamePasswordMatch());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testForbiddenPassword() {
        values.setUserName("willFail");
        values.setPassword("administrator".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustNotBeEqual("administrator"));
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testWeakPassword() {
        values.setUserName("willFail");
        values.setPassword("zxcvbnm1@".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordNotStrongEnough("MODERATE", "MEDIUM"));
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testTooShortPassword() {
        values.setUserName("willFail");
        values.setPassword("1QwD%rf".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordNotLontEnough(8));
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testNoDigitInPassword() {
        values.setUserName("willFail");
        values.setPassword("!QwD%rGf".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustHaveDigit());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testNoSymbolInPassword() {
        values.setUserName("willFail");
        values.setPassword("1QwD5rGf".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustHaveSymbol());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

    @Test
    public void testNoAlphaInPassword() {
        values.setUserName("willFail");
        values.setPassword("1$*>5&#}".toCharArray());
        WeakPasswordCheckState weakPasswordCheckState = new WeakPasswordCheckState(consoleMock, values);

        AssertConsoleBuilder consoleBuilder = new AssertConsoleBuilder().expectedErrorMessage(MESSAGES.passwordMustHaveAlpha());
        consoleMock.setResponses(consoleBuilder);

        State errorState = weakPasswordCheckState.execute();

        assertTrue("Expected the next state to be ErrorState", errorState instanceof ErrorState);
        State promptPasswordState = errorState.execute();
        assertTrue("Expected the next state to be PromptPasswordState", promptPasswordState instanceof PromptPasswordState);
        consoleBuilder.validate();
    }

}
