package main
/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import (
	"testing"
	"strings"
	"net/http/httptest"
	"io"
	"os"
	"fmt"
)

const timestamp_2 = 2000
const configuration_1 = `{"timestamp":1, "configuration":"first yaml config"}`
const configuration_2 = `{"timestamp":2000, "configuration":"second yaml config"}`
const no_timestamp_configuration = `{"configuration":"bogus config"}`
const no_configuration_json = `{"timestamp":3000, "config":"bogus config"}`
const bad_json = `{"timestamp":3000, `

var savedOpenFunc func(path string) (io.ReadCloser, error)
var savedCreateFunc func(path string) (io.WriteCloser, error)
var savedLogMessage func(format string, v ...interface{})

var messages []string

var openedFileName string
var wasClosed bool
var createdFilePath string
var createdFile *InMemoryFile

const dbSwitch = "-" + db_flag

func setUp() func() {
	args = nil
	wasClosed = false
	createdFilePath = ""
	createdFile = nil

	savedOpenFunc = openFile
	savedCreateFunc = createFile
	savedLogMessage = logMessage

	messages = []string{}
	logMessage = func(format string, v ...interface{}) {
		messages = append(messages, fmt.Sprintf(format, v...))
	}

	initialize()

	return func() {
		openFile = savedOpenFunc
		createFile = savedCreateFunc
		logMessage = savedLogMessage
	}
}

// Tests that calling 'putConfiguration' with bad JSON returns an error
func TestGetWithNoConfiguration_returnsEmptyConfig(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	if string(getConfiguration()) != empty_configuration {
		t.Errorf("Expected %s but found <%s>", empty_configuration, getConfiguration())
	}
}

// Tests that calling 'putConfiguration' with bad JSON returns an error
func TestPutWithBadJSON_returnsErr(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	err := putConfiguration([]byte(bad_json))

	if err == nil {
		t.Errorf("Expected to receive error message")
	}
}

// Tests that an http PUT request which fails to specify a configuration is flagged as an error
func TestPutWithMissingConfiguration_returnsErr(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	err := putConfiguration([]byte(no_configuration_json))

	if err == nil {
		t.Errorf("Expected to receive error message")
	}
}

// Tests that calling 'putConfiguration' updates the global state that 'getConfiguration' retrieves
func TestPutUpdatesConfiguration(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	putConfiguration([]byte(configuration_1))

	if string(getConfiguration()) != configuration_1 {
		t.Errorf("Expected %s but found <%s>", configuration_1, getConfiguration())
	}
}

// Tests that a 'putConfiguration' call which includes a timestamp earlier than the latest is ignored
func TestDontUpdateWithEarlierConfiguration(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	putConfiguration([]byte(configuration_2))
	putConfiguration([]byte(configuration_1))

	if string(getConfiguration()) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, getConfiguration())
	}
}

// Tests that an http PUT request updates the configuration returned by a GET request
func TestHttpPutUpdatesConfiguration(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

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
	tearDown := setUp()
	defer tearDown()

	doPutRequest(configuration_2)
	doPutRequest(configuration_1)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that an http PUT request which fails to specify a configuration is ignored
func TestHttpIgnoreMissingConfiguration(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	doPutRequest(configuration_2)
	doPutRequest(no_configuration_json)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that an http PUT request which fails to specify a timestamp is ignored
func TestHttpIgnoreMissingTimestamp(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	doPutRequest(configuration_2)
	doPutRequest(no_timestamp_configuration)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that an http PUT request with bad JSON is ignored
func TestHttpIgnoreBadJSON(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	doPutRequest(configuration_2)
	doPutRequest(bad_json)

	latest := doGetRequest()

	if string(latest) != configuration_2 {
		t.Errorf("Expected %s but found <%s>", configuration_2, latest)
	}
}

// Tests that when no file is found, the initial configuration is set to the empty configuration

func TestAfterInitializationWithNoFile_stateIsEmpty(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	if string(latest_configuration) != empty_configuration {
		t.Errorf("Expected %s but found <%s>", empty_configuration, string(latest_configuration))
	}
}

// Tests that when no file is found, the initial configuration is set to the empty configuration

func TestAfterInitializationWithNoFile_timestampIsZero(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	if latest_timestamp != 0 {
		t.Errorf("Expected 0 but found <%d>", latest_timestamp)
	}
}

func TestSelectDefaultPort(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	args = []string{""}

	readCommandLine()

	if serverAddress != ":8999" {
		t.Errorf("Expected %s but found <%s>", ":8999", serverAddress)
	}
}

func TestSelectNonDefaultPort(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	args = []string{"-port", "5000"}

	readCommandLine()

	if serverAddress != ":5000" {
		t.Errorf("Expected %s but found <%s>", ":5000", serverAddress)
	}
}

func TestWhenPersistentFileSpecified_readIt(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	const filePath = "/data/config.json"

	args = []string{dbSwitch, filePath}
	installInMemoryFile(configuration_2)

	initialize()

	if openedFileName != filePath {
		t.Errorf("Expected to open config.json but opened <%s>", openedFileName)
	} else if string(latest_configuration) != configuration_2 {
		t.Errorf("Expected configuration %s but found <%s>", configuration_2, latest_configuration)
	} else if latest_timestamp != timestamp_2 {
		t.Errorf("Expected timestamp %d but found %d", timestamp_2, latest_timestamp)
	} else if !wasClosed {
		t.Errorf("File was not closed")
	}
}

func installInMemoryFile(configurationString string) {
	openFile = func(path string) (io.ReadCloser, error) {
		openedFileName = path
		return newExistingInMemoryFile(configurationString), nil
	}
}

func TestFileNotFound_logErrorAndUseDefaultConfiguration(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	args = []string{dbSwitch, "config.json"}
	installNoSuchFile()

	initialize()

	if string(latest_configuration) != empty_configuration {
		t.Errorf("Expected configuration %s but found <%s>", empty_configuration, latest_configuration)
	}
}

func installNoSuchFile() {
	openFile = func(path string) (io.ReadCloser, error) {
		return nil, os.ErrNotExist
	}
}

func TestWhenFileDefined_updateWritesConfiguration(t *testing.T) {
	tearDown := setUp()
	defer tearDown()

	const filePath = "/data/config.json"

	args = []string{dbSwitch, filePath}
	initialize()

	installReadyToWriteFile()
	doPutRequest(configuration_1)

	if createdFile == nil {
		t.Errorf("Persistant file not created")
	} else if createdFilePath != filePath {
		t.Errorf("Expected file path %s but found <%s>", filePath, createdFilePath)
	} else if string(createdFile.contents) != configuration_1 {
		t.Errorf("Expected file contents to be %s but found <%s>", configuration_1, string(createdFile.contents))
	} else if !wasClosed {
		t.Errorf("Persistant file not closed")
	}

}

func installReadyToWriteFile() {
	createFile = func(path string) (io.WriteCloser, error) {
		createdFilePath = path
		createdFile = newCreatedInMemoryFile()
		return createdFile, nil
	}
}

//----------------

type InMemoryFile struct {
	index int
	contents []byte
}

func (f *InMemoryFile) Read(p []byte) (n int, err error) {
	size, err := results(len(f.contents) - f.index, len(p))
	copy(p, f.contents[f.index:f.index + size])
	f.index += size

	return size, err
}

// computes the return values for the Read method, reporting an EOF if the number of bytes left to read
// is less than the size of the buffer into which to read them
func results(bytes_left, buffer_size int) (int, error) {
	if bytes_left < buffer_size {
		return bytes_left, io.EOF
	} else {
		return buffer_size, nil
	}
}

func (f *InMemoryFile) Close() error {
	wasClosed = true
	return nil
}

func (f *InMemoryFile) Write(p []byte) (n int, err error) {
	f.contents = p
	return len(p), nil
}


func newExistingInMemoryFile(contents string) *InMemoryFile {
	return &InMemoryFile{0, []byte(contents)}
}

func newCreatedInMemoryFile() *InMemoryFile {
	return &InMemoryFile{0, nil}
}




