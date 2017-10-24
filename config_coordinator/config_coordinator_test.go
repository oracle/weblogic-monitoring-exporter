package main
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import (
	"testing"
	"strings"
	"net/http/httptest"
)

const configuration_1 = `{"timestamp":1, "configuration":"first yaml config"}`
const configuration_2 = `{"timestamp":2, "configuration":"second yaml config"}`
const no_timestamp_configuration = `{"configuration":"bogus config"}`
const no_configuration_json = `{"timestamp":3, "config":"bogus config"}`
const bad_json = `{"timestamp":3, `

func setUp() {
	latest_timestamp = 0
}

// Tests that calling 'putConfiguration' with bad JSON returns an error
func TestGetWithNoConfiguration_returnsEmptyConfig(t *testing.T) {
	setUp()
	if string(getConfiguration()) != empty_configuration {
		t.Errorf("Expected %s but found <%s>", empty_configuration, getConfiguration())
	}
}

// Tests that calling 'putConfiguration' with bad JSON returns an error
func TestPutWithBadJSON_returnsErr(t *testing.T) {
	setUp()
	err := putConfiguration([]byte(bad_json))

	if err == nil {
		t.Errorf("Expected to receive error message")
	}
}

// Tests that an http PUT request which fails to specify a configuration is flagged as an error
func TestPutWithMissingConfiguration_returnsErr(t *testing.T) {
	setUp()
	err := putConfiguration([]byte(no_configuration_json))

	if err == nil {
		t.Errorf("Expected to receive error message")
	}
}

// Tests that calling 'putConfiguration' updates the global state that 'getConfiguration' retrieves
func TestPutUpdatesConfiguration(t *testing.T) {
	setUp()
	putConfiguration([]byte(configuration_1))

	if string(getConfiguration()) != configuration_1 {
		t.Errorf("Expected %s but found <%s>", configuration_1, getConfiguration())
	}
}

// Tests that a 'putConfiguration' call which includes a timestamp earlier than the latest is ignored
func TestDontUpdateWithEarlierConfiguration(t *testing.T) {
	setUp()
	putConfiguration([]byte(configuration_2))
	putConfiguration([]byte(configuration_1))

	if string(getConfiguration()) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, getConfiguration())
	}
}

// Tests that an http PUT request updates the configuration returned by a GET request
func TestHttpPutUpdatesConfiguration(t *testing.T) {
	setUp()
	doPutRequest(configuration_1)

	latest := doGetRequest()

	if string(latest) != configuration_1 {
		t.Errorf("Expected %s but found <%s>", configuration_1, latest)
	}

}

func doPutRequest(contents string) []byte {
	return doRequest("PUT", contents)
}

func doGetRequest() []byte {
	return doRequest("GET", "")
}

func doRequest(method, contents string) []byte {
	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(method, "http://configurations:8999", strings.NewReader(contents))

	handler(recorder, request)

	return recorder.Body.Bytes()
}

// Tests that an http PUT request which specifies an earlier timestamp is ignored
func TestHttpDontOverrideLatest(t *testing.T) {
	setUp()
	doPutRequest(configuration_2)
	doPutRequest(configuration_1)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that an http PUT request which fails to specify a configuration is ignored
func TestHttpIgnoreMissingConfiguration(t *testing.T) {
	setUp()
	doPutRequest(configuration_2)
	doPutRequest(no_configuration_json)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that an http PUT request which fails to specify a timestamp is ignored
func TestHttpIgnoreMissingTimestamp(t *testing.T) {
	setUp()
	doPutRequest(configuration_2)
	doPutRequest(no_timestamp_configuration)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that an http PUT request with bad JSON is ignored
func TestHttpIgnoreBadJSON(t *testing.T) {
	setUp()
	doPutRequest(configuration_2)
	doPutRequest(bad_json)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}
