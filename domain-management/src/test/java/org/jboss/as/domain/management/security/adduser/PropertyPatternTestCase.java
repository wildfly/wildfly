package org.jboss.as.domain.management.security.adduser;

import org.jboss.as.domain.management.security.PropertiesFileLoader;
import org.jboss.as.domain.management.security.UserPropertiesFileLoader;
import org.jboss.msc.service.StartException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PropertyPatternTestCase {

    @Test
    public void testPropertyPatternMatcher() throws IOException {
        List<String> content = Arrays.asList(
                "#Guillaume.Grossetie=developer",
                "Aldo.Raine@Inglorious-Basterds.com=sergent,2division",
                "# Comment",
                "",
                "#",
                "#Omar.Ulmer=soldier"
        );
        List<String> keys = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        Pattern propertyPattern = PropertiesFileLoader.PROPERTY_PATTERN;
        for (String line : content) {
            Matcher matcher = propertyPattern.matcher(line.trim());
            if (matcher.matches()) {
                keys.add(matcher.group(1));
                values.add(matcher.group(2));
            }
        }
        assertEquals(3, keys.size());
        assertEquals("Guillaume.Grossetie", keys.get(0));
        assertEquals("Aldo.Raine@Inglorious-Basterds.com", keys.get(1));
        assertEquals("Omar.Ulmer", keys.get(2));

        assertEquals(3, values.size());
        assertEquals("developer", values.get(0));
        assertEquals("sergent,2division", values.get(1));
        assertEquals("soldier", values.get(2));
    }

    @Test
    public void testLoadEnabledDisabledUsers() throws IOException, StartException {
        File usersPropertyFile = File.createTempFile("User", null);
        usersPropertyFile.deleteOnExit();
        List<String> content = Arrays.asList(
                "#Guillaume.Grossetie=abc123",
                "Aldo.Raine@Inglorious-Basterds.com=def456",
                "# Comment",
                "",
                "#",
                "#Omar.Ulmer=ghi789"
        );
        PrintWriter writer = new PrintWriter(usersPropertyFile, "UTF-8");
        try {
            for (String line : content) {
                writer.println(line);
            }
        } finally {
            writer.close();
        }
        UserPropertiesFileLoader propertiesLoad = new UserPropertiesFileLoader(usersPropertyFile.getAbsolutePath());
        propertiesLoad.start(null);
        assertEquals(1, propertiesLoad.getEnabledUserNames().size());
        assertEquals(2, propertiesLoad.getDisabledUserNames().size());
        assertEquals(3, propertiesLoad.getUserNames().size());
        assertTrue(propertiesLoad.getEnabledUserNames().contains("Aldo.Raine@Inglorious-Basterds.com"));
        assertTrue(propertiesLoad.getDisabledUserNames().contains("Guillaume.Grossetie"));
        propertiesLoad.stop(null);
    }
}
