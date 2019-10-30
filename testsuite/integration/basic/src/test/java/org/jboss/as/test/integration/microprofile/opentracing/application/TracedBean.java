package org.jboss.as.test.integration.microprofile.opentracing.application;

import org.eclipse.microprofile.opentracing.Traced;

@Traced
public class TracedBean {
    public void doSomething() {
    }

    @Traced(true)
    public void doSomethingElse() {
    }

    @Traced(false)
    public void doYetAnotherSomething() {
    }
}
