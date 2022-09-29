package org.jboss.as.ee.component;

import org.jboss.msc.service.ServiceName;
import org.junit.Test;


import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class EEModuleDescriptionUnitTestCase {

    @Test
    public void testAddExistingComponent() {
        EEModuleDescription eeModuleDescription = new EEModuleDescription("appName", "module1",
                "ear1", false);
        ComponentDescription description1 = new ComponentDescription("comp1",
                "org.test.comp1",
                eeModuleDescription,
                ServiceName.of("name"));
        ComponentDescription description2 = new ComponentDescription("comp1",
                "org.test.comp2",
                eeModuleDescription,
                ServiceName.of("name"));

        eeModuleDescription.addComponent(description1);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            eeModuleDescription.addComponent(description2);
        });

        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains("WFLYEE0040"));
        assertTrue(actualMessage.contains("comp1"));
        assertTrue(actualMessage.contains("org.test.comp2"));
        assertTrue(actualMessage.contains("org.test.comp1"));
        assertTrue(exception instanceof IllegalArgumentException);

    }

}
