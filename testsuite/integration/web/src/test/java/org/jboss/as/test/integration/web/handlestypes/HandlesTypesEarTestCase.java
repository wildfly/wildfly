package org.jboss.as.test.integration.web.handlestypes;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.ServletContainerInitializer;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class HandlesTypesEarTestCase {

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "test.war")
                .addClasses(SomeAnnotation.class, HandlesTypesEarTestCase.class)
                .addClasses(NonAnnotatedChild.class, AnnotatedChild.class)
                .addClasses(HandlesTypesChild.class, HandlesTypesGandchild.class)
                .addClasses(HandlesTypesImplementor.class, HandlesTypesImplementorChild.class)
                .addClasses(ParentServletContainerInitializer.class, ChildServletContainerInitializer.class, AnnotationServletContainerInitializer.class)
                .addAsServiceProvider(ServletContainerInitializer.class, ParentServletContainerInitializer.class, ChildServletContainerInitializer.class, AnnotationServletContainerInitializer.class);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "lib.jar")
                .addClasses(AnnotatedParent.class, HandlesTypesInterface.class, HandlesTypesParent.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "sci.ear")
                .addAsLibrary(jar)
                .addAsModule(war);

        return ear;
    }

    @Test
    public void testParentClass() {
        Class<?>[] expeccted = {HandlesTypesChild.class, HandlesTypesImplementor.class, HandlesTypesGandchild.class, HandlesTypesImplementorChild.class};
        Assert.assertEquals(new HashSet<>(Arrays.asList(expeccted)), ParentServletContainerInitializer.HANDLES_TYPES);
    }

    @Test
    public void testChildClass() {
        Class<?>[] expeccted = {HandlesTypesGandchild.class, HandlesTypesImplementorChild.class};
        Assert.assertEquals(new HashSet<>(Arrays.asList(expeccted)), ChildServletContainerInitializer.HANDLES_TYPES);
    }

    @Test
    public void testAnnotatedClass() {
        Class<?>[] expeccted = {AnnotatedParent.class, AnnotatedChild.class};
        Assert.assertEquals(new HashSet<>(Arrays.asList(expeccted)), AnnotationServletContainerInitializer.HANDLES_TYPES);
    }
}
