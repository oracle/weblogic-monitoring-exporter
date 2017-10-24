package main
/*
 * Copyright (c) 2017 Oracle and/or its affiliates
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

import (
	"encoding/json"
	"errors"
	"io/ioutil"
	"log"
	"net/http"
	"sync"
)

func main() {
	http.HandleFunc("/", handler)
	log.Fatal(http.ListenAndServe(serverAddress, nil))
}


// The structure of the JSON objects exchanged with clients
type config struct {
	Timestamp int
	Configuration string
}

const serverAddress = ":8999"
const empty_configuration = `{"timestamp":0, "configuration":""}`

var (
	rw                   sync.RWMutex
	latest_timestamp     int
	latest_configuration []byte = []byte(empty_configuration)
)

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
		latest_timestamp = config.Timestamp
		latest_configuration = configuration
	}
	return nil
}

func getConfiguration() []byte {
	rw.RLock()
	configuration := latest_configuration
	rw.RUnlock()
	return configuration
}
