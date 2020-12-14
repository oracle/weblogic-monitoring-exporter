// Copyright (c) 2019, 2020, Oracle and/or its affiliates.
// Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.

package com.oracle.wls.exporter;

class ErrorLog {
    private StringBuilder errors = new StringBuilder();

    /**
     * Report an exception.
     * @param throwable the exception
     */
    void log(Throwable throwable) {
        errors.append(toLogMessage(throwable));
        for (Throwable cause = throwable.getCause(); cause != null; cause = cause.getCause())
            errors.append(System.lineSeparator()).append("  ").append(toLogMessage(cause));
    }

    private String toLogMessage(Throwable throwable) {
        StringBuilder sb = new StringBuilder(throwable.getClass().getSimpleName());
        if (throwable.getMessage() != null && throwable.getMessage().trim().length() > 0)
            sb.append(": ").append(throwable.getMessage());

        return sb.toString();
    }

    /**
     * returns the current error log.
     * @return a string of errors, separated by line breaks.
     */
    String getErrors() {
        return errors.toString();
    }
}
