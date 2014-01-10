package org.jboss.as.ejb3.deployment.processors;

import groovy.lang.MetaClass;
import java.util.Set;
import javax.jms.MessageListener;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link ViewInterfaces}
 *
 * @author Filipe Ferraz
 * @version $Revision: $
 */
public class ViewInterfacesTestCase {

    /**
     * Tests that the {@link ViewInterfaces#getPotentialViewInterfaces(Class<?>)} returns the correct
     * implementation class for java class implementing MessageListener interface.
     */
    @Test
    public void testJavaClass() {
        Set javaClasses = ViewInterfaces.getPotentialViewInterfaces(TestJavaMessageListener.class);

        Assert.assertEquals("One object epected in Java class", 1, javaClasses.size());
        Assert.assertEquals("Expected interface in Java class", MessageListener.class, javaClasses.iterator().next());
    }

    /**
     * Tests that the {@link ViewInterfaces#getPotentialViewInterfaces(Class<?>)} returns the correct
     * implementation class for groovy class implementing MessageListener interface.
     */
    @Test
    public void testGroovyClass() {
        Set groovyClasses = ViewInterfaces.getPotentialViewInterfaces(TestGroovyMessageListener.class);

        Assert.assertEquals("One object epected in Groovy class", 1, groovyClasses.size());
        Assert.assertEquals("Expected interface in Groovy class", MessageListener.class, groovyClasses.iterator().next());
    }

    private abstract class TestJavaMessageListener implements MessageListener {
    }

    private abstract class TestGroovyMessageListener implements MessageListener, MetaClass {
    }

}
