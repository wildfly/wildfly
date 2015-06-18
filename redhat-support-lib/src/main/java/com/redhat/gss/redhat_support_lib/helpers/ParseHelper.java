package com.redhat.gss.redhat_support_lib.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.redhat.gss.redhat_support_lib.parsers.LinkType;
import com.redhat.gss.redhat_support_lib.parsers.ProblemType;
import com.redhat.gss.redhat_support_lib.parsers.ProblemsType;

public class ParseHelper {
    public static List<LinkType> getLinksFromProblems(ProblemsType probs) {
        List<LinkType> links = new ArrayList<LinkType>();
        for (Serializable prob : probs.getSourceOrLinkOrProblem()) {
            if (prob instanceof ProblemType) {
                for (Serializable link : ((ProblemType) prob).getSourceOrLink()) {
                    if (link instanceof LinkType) {
                        links.add((LinkType) link);
                    }
                }
            }
        }
        return links;
    }

    public static Properties parseConfigFile(String fileName)
            throws IOException {
        Properties prop = new Properties();
        InputStream is = new FileInputStream(fileName);
        prop.load(is);
        return prop;
    }
}
