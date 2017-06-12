package org.jboss.as.test.integration.deployment.classloading.WFLY_8907;

import java.util.function.Function;
import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless
@Remote(Function.class)
public class CalleeBean implements Function<String, MyObject> {

    @Override
    public MyObject apply(String stuff) {
        return new MyObject(stuff);
    }
}
