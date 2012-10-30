package org.jboss.as.domain.management.security.state;

import org.jboss.as.domain.management.security.ConsoleWrapper;
import org.jboss.as.domain.management.security.password.PasswordCheckResult;
import org.jboss.as.domain.management.security.password.PasswordCheckUtil;

import java.util.Arrays;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

/**
 * State to check the strength of the stateValues selected.
 * <p/>
 * TODO - Currently only very basic checks are performed, this could be updated to perform additional password strength
 * checks.
 */
public class WeakPasswordCheckState implements State {

    private ConsoleWrapper theConsole;
    private StateValues stateValues;

    public WeakPasswordCheckState(ConsoleWrapper theConsole, StateValues stateValues) {
        this.theConsole = theConsole;
        this.stateValues = stateValues;
        if ((stateValues != null && stateValues.isSilent() == false) && theConsole.getConsole() == null) {
            throw MESSAGES.noConsoleAvailable();
        }
    }

    @Override
    public State execute() {
        State retryState = stateValues.isSilentOrNonInteractive() ? null : new PromptPasswordState(theConsole, stateValues);

        if (Arrays.equals(stateValues.getUserName().toCharArray(), stateValues.getPassword())) {
            return new ErrorState(theConsole, MESSAGES.usernamePasswordMatch(), retryState, stateValues);
        }

        State continuingState = new DuplicateUserCheckState(theConsole, stateValues);

        PasswordCheckResult result = PasswordCheckUtil.INSTANCE.check(false, stateValues.getUserName(), new String(stateValues.getPassword()));
        if (result.getResult() == PasswordCheckResult.Result.WARN && stateValues.isSilentOrNonInteractive() == false) {
            String message = result.getMessage();
            String prompt = MESSAGES.sureToSetPassword(new String(stateValues.getPassword()));
            State noState = new PromptPasswordState(theConsole, stateValues);
            return new ConfirmationChoice(theConsole, message, prompt, continuingState, noState);
        }

        if (result.getResult() == PasswordCheckResult.Result.REJECT) {
            return new ErrorState(theConsole, result.getMessage(), retryState);
        }

        return continuingState;
    }

}