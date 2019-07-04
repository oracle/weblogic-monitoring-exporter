package io.prometheus.wls.rest.matchers;
/*
 * Copyright (c) 2017, 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.time.Instant;

/**
 * A matcher that verifies that a specified instant is within the desired range
 */
public class InstantRangeMatcher extends TypeSafeDiagnosingMatcher<Instant> {
    private Instant start;
    private Instant end;

    private InstantRangeMatcher(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    public static InstantRangeMatcher inRange(Instant start, Instant end) {
        return new InstantRangeMatcher(start, end);
    }

    @Override
    protected boolean matchesSafely(Instant instant, Description description) {
        if (inOrder(start, instant) && inOrder(instant, end)) return true;

        description.appendValue(instant);
        return false;
    }

    private boolean inOrder(Instant first, Instant second) {
        return first.compareTo(second) <= 0;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an instant in the range ").appendValue(start).appendText(" to ").appendValue(end);
    }
}
