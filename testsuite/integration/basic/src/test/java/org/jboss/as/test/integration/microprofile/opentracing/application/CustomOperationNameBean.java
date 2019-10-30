package org.jboss.as.test.integration.microprofile.opentracing.application;

import org.eclipse.microprofile.opentracing.Traced;

@Traced(operationName = "my-custom-class-operation-name")
public class CustomOperationNameBean {

    @Traced(operationName = "my-custom-method-operation-name")
    public void doSomething() {
    }

    @Traced
    public void doSomethingElse() {
    }
}
