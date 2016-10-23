package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import javax.ejb.Timer;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * Checking values of InvocationContext during its flow amongst interceptors.
 *
 * @author Ondrej Chaloupka
 */
public class InvocationContextChecker {

    private static final Logger log = Logger.getLogger(InvocationContextChecker.class);

    public static String checkBeanInterceptorContext(InvocationContext ctx, String previousPhase, String currentPhase) {
        log.trace("Checking method call interceptor on: " + currentPhase);
        boolean okContext = false;
        if (previousPhase == null) {
            okContext = ctx.getContextData().get("interceptor") == null;
        } else {
            okContext = previousPhase.equals(ctx.getContextData().get("interceptor"));
        }
        ctx.getContextData().put("interceptor", currentPhase);
        final boolean okTimer = ctx.getTimer() == null;
        final boolean okTarget = ctx.getTarget() instanceof InvocationBean;
        final boolean okMethod = "callMethod".equals(ctx.getMethod().getName());

        Object[] params = ctx.getParameters();
        Integer param1 = (Integer) params[0];
        String param2 = (String) params[1];
        final boolean okParam = param1 == 1;
        Object[] newParams = {param1, param2 + currentPhase};
        ctx.setParameters(newParams);

        String retStr = currentPhase;
        boolean isOk = okContext && okTimer && okTarget && okMethod && okParam;
        if (isOk) {
            retStr += "OK:";
        } else {
            retStr += "FAIL:";
            retStr += okContext ? "" : "(context expected: " + previousPhase + " but was " + ctx.getContextData().get("interceptor") + ")";
            retStr += okTimer ? "" : "(timer was not null but " + ctx.getTimer() + " )";
            retStr += okTarget ? "" : "(target was not instance of InvocationBean but was " + ctx.getTarget() + ")";
            retStr += okMethod ? "" : "(method was not callMethod but was " + ctx.getMethod().getName() + ")";
            retStr += okParam ? "" : "(first parameter was not 1 but was " + param1 + ")";
            log.error(retStr);
        }
        return retStr;
    }

    public static String checkTimeoutInterceptorContext(InvocationContext ctx, String previousPhase, String currentPhase) {
        log.trace("Checking timeout interceptor on: " + currentPhase);
        boolean okContext = false;
        if (previousPhase == null) {
            okContext = ctx.getContextData().get("interceptor") == null;
        } else {
            okContext = previousPhase.equals(ctx.getContextData().get("interceptor"));
        }
        ctx.getContextData().put("interceptor", currentPhase);
        final boolean okTimer = ctx.getTimer() != null;
        final boolean okTarget = ctx.getTarget() instanceof TimeoutBean;
        final boolean okMethod = "timeout".equals(ctx.getMethod().getName());
        Object[] params = ctx.getParameters();
        final boolean okParams = params[0] instanceof Timer;

        String retStr = "Timeout" + currentPhase;
        boolean isOk = okContext && okTimer && okTarget && okMethod && okParams;
        if (isOk) {
            retStr += "OK:";
        } else {
            retStr += "FAIL:";
            retStr += okContext ? "" : "(context expected: " + previousPhase + " but was " + ctx.getContextData().get("interceptor") + ")";
            retStr += okTimer ? "" : "(timer was null but it can't be)";
            retStr += okTarget ? "" : "(target was not instance of InvocationBean but was " + ctx.getTarget() + ")";
            retStr += okMethod ? "" : "(method was not callMethod but was " + ctx.getMethod().getName() + ")";
            retStr += okParams ? "" : "(first param has to be type of Timer and not " + params[0] + " )";
            log.error(retStr);
        }
        return retStr;
    }
}
