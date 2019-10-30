package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class Request implements Externalizable {

    private String greeting;
    private transient ClassLoader tccl;

    public Request() {
    }

    public Request(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }

    public ClassLoader getTccl() {
        return tccl;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(greeting);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        greeting = (String) in.readObject();
        tccl = Thread.currentThread().getContextClassLoader(); // save a reference to current TCCL
    }
}
