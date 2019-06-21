/*
 * Copyright (c) 2019, Oracle Corporation and/or its affiliates. All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package io.prometheus.wls.rest;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.prometheus.wls.rest.ErrorLogTest.LogMatcher.containsErrors;
import static org.hamcrest.junit.MatcherAssert.assertThat;

public class ErrorLogTest {
    private ErrorLog errorLog = new ErrorLog();

    @Test
    public void afterExceptionReported_isAddedToLog() {
        errorLog.log(new IOException("Unable to read value"));

        assertThat(errorLog, containsErrors("IOException: Unable to read value"));
    }

    @Test
    public void afterExceptionWithNoMessageReported_logSimpleNameOnly() {
        errorLog.log(new IOException());

        assertThat(errorLog, containsErrors("IOException"));
    }

    @Test
    public void afterExceptionWithNestedThrowableReported_addToLog() {
        errorLog.log(new IOException("Unable to read value", new RuntimeException("has impossible format")));

        assertThat(errorLog, containsErrors("IOException: Unable to read value", "  RuntimeException: has impossible format"));
    }

    @SuppressWarnings("unused")
    static class LogMatcher extends TypeSafeDiagnosingMatcher<ErrorLog> {
        private String[] expectedMessages;

        private LogMatcher(String[] expectedMessages) {
            this.expectedMessages = expectedMessages;
        }

        static LogMatcher containsErrors(String... expectedMessages) {
            return new LogMatcher(expectedMessages);
        }

        @Override
        protected boolean matchesSafely(ErrorLog errorLog, Description description) {
            String[] actualMessages = getActualMessages(errorLog);
            if (Arrays.equals(expectedMessages, actualMessages)) return true;

            description.appendValueList("[", ",", "]", actualMessages);
            return false;
        }

        private String[] getActualMessages(ErrorLog errorLog) {
            try {
                List<String> actualMessages = new ArrayList<>();
                String line;
                BufferedReader reader = new BufferedReader(new StringReader(errorLog.getErrors()));
                while ((line = reader.readLine()) != null) actualMessages.add(line);
                return actualMessages.toArray(new String[0]);
            } catch (IOException e) {
                return new String[0];
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendValueList("[", ",", "]", expectedMessages);
        }
    }
}
