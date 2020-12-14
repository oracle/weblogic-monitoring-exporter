package main
/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 */

import (
	"encoding/json"
	"errors"
	"io/ioutil"
	"log"
	"net/http"
	"sync"
	"flag"
	"fmt"
	"os"
	"io"
)

func main() {
	log.Printf("Starting coordinator")
	initialize()

	log.Printf("Listening on %s", serverAddress)
	http.HandleFunc("/", handler)
	log.Fatal(http.ListenAndServe(serverAddress, nil))
}


// The structure of the JSON objects exchanged with clients
type config struct {
	Timestamp int
	Configuration string
}

const defaultServerAddress = 8999
const empty_configuration = `{"timestamp":0, "configuration":""}`

const port_flag = "port"
const db_flag = "db"

var (
	rw                   sync.RWMutex
	latest_timestamp     int
	latest_configuration []byte

	serverAddress string

	args []string = os.Args[1:]
)

var portFlag = flag.Int(port_flag, defaultServerAddress, "the port on which the coordinator should listen")
var dbFlag = flag.String(db_flag, "", "a file in which to persist the latest state")

var openFile = func(path string) (io.ReadCloser, error) {
	return os.Open(path)
}

var createFile = func(path string) (io.WriteCloser, error) {
	return os.Create(path)
}

var logMessage = func(format string, v ...interface{}) {
	log.Printf(format, v...)
}

/*
  Initialization of the coordinator
 */
func initialize() {
	readCommandLine()

	if *dbFlag == "" {
		installEmptyConfiguration()
	} else {
		logMessage("Trying to load configuration from %s", *dbFlag)
		file, err := openFile(*dbFlag)
		if err != nil {
			installEmptyConfiguration()
		} else {
			putConfiguration(readFully(file))
		}
	}

}
func installEmptyConfiguration() {
	latest_configuration = []byte(empty_configuration)
	latest_timestamp = 0
}

// Returns the contents of the file as a byte array.
// Any errors merely truncate the contents but are otherwise ignored
func readFully(file io.ReadCloser) []byte {
	defer file.Close()

	var err error
	var n int
	result := make([]byte, 0)
	buffer := make([]byte, 256)
	for err == nil {
		n, err = file.Read(buffer)
		result = append(result, buffer[0:n]...)
	}
	return result
}

func readCommandLine() {
	flag.CommandLine.Parse(args)
	serverAddress = fmt.Sprintf(":%d", *portFlag)
}

/*
  A handler for HTTP requests.
  A PUT request updates the latest configuration if its timestamp is higher than
      the latest timestamp, and is otherwise ignored.
  A GET request unconditionally returns the latest configuration
 */
func handler(writer http.ResponseWriter, request *http.Request) {
	switch request.Method {
	case "GET":
		writer.Write(getConfiguration())
	case "PUT":
		contents, err := ioutil.ReadAll(request.Body)
		if err != nil {
			reportError(writer, err, http.StatusInternalServerError)
		} else {
			handlePutRequest(writer, contents)
		}
	}
}

func reportError(writer http.ResponseWriter, err error, errorCode int) {
	writer.Write([]byte(err.Error()))
	writer.WriteHeader(errorCode)
}

func handlePutRequest(writer http.ResponseWriter, contents []byte) {
	err := putConfiguration(contents)
	if err != nil {
		reportError(writer, err, http.StatusBadRequest)
	}
}

func putConfiguration(configuration []byte) error {
	var config config
	err := json.Unmarshal(configuration, &config)
	defer rw.Unlock()
	rw.Lock()

	if err != nil {
		return err
	} else if config.Configuration == "" {
		return errors.New("no configuration element present")
	} else if config.Timestamp > latest_timestamp {
		recordNewConfiguration(config, configuration)
	}
	return nil
}

func recordNewConfiguration(config config, configuration []byte) {
	latest_timestamp = config.Timestamp
	latest_configuration = configuration

	file, err := createFile(*dbFlag)
	reportWriteError(err)

	_, err = file.Write(configuration)
	reportWriteError(err)

	file.Close()
}

func reportWriteError(err error) {
	if err != nil {
		logMessage("unable to perform write: %v", err)
	}
}

func getConfiguration() []byte {
	defer rw.RUnlock()
	rw.RLock()

	return latest_configuration
}
