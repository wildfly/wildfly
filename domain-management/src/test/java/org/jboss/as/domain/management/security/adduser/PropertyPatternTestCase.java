package org.jboss.as.domain.management.security.adduser;

import org.jboss.as.domain.management.security.PropertiesFileLoader;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

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

    protected String getUserName(String line) {
        final String userName;
        int separatorIndex = line.indexOf('=');
        if (line.startsWith("#")) {
            userName = line.substring(1, separatorIndex);
        } else {
            userName = line.substring(0, separatorIndex);
        }
        return userName;
    }
}
