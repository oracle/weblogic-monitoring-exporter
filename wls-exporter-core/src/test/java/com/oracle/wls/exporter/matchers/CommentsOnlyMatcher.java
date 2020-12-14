// Copyright (c) 2017, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * A matcher which confirms that the text contains only comments (lines starting with '#')
 *
 * @author Russell Gold
 */
public class CommentsOnlyMatcher extends TypeSafeDiagnosingMatcher<String> {

    public static CommentsOnlyMatcher containsOnlyComments() {
        return new CommentsOnlyMatcher();
    }

    @Override
    protected boolean matchesSafely(String s, Description description) {
        String[] lines = s.split("\n");
        for (int i = 0; i < lines.length; i++)
            if (!lines[i].trim().startsWith("#")) return foundNonCommentLine(description, i, lines[i]);
        return true;
    }

    private boolean foundNonCommentLine(Description description, int lineNum, String line) {
        description.appendText("Found non-comment @line ")
                .appendText(Integer.toString(lineNum))
                .appendText("\n")
                .appendValue(line);
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("only lines beginning with '#'");
    }
}
