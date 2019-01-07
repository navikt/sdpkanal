# SDPkanal

[![Build Status](https://travis-ci.org/navikt/sdpkanal.svg?branch=master)](https://travis-ci.org/navikt/sdpkanal)

SDPkanal is an application that handles encryping document packages, signing payload and EBMS when sending messages
through Sikker Digital Post. It has retrofitted postens [sdp-shared](https://github.com/digipost/sdp-shared) and difi's
[sikker-digital-post-klient-java](https://github.com/difi/sikker-digital-post-klient-java/) to avoid using the
proprietary crypto and ebms libraries used earlier.

## Technologies & Tools

* [Kotlin](https://kotlinlang.org)
* [CXF](https://cxf.apache.org)
* [Jetty](https://eclipse.org/jetty)
* [Gradle](https://gradle.org)
* [Spek](http://spekframework.org)

## Getting started

### Compile, build and run tests
`./gradlew clean build`

### Generate uber jar:
`./gradlew shadowJar`

You will then find the application file in `build/libs/sdpkanal-VERSION.jar`

### Running locally
To run it locally you can set up an instance of IBM mq and configure the required environment variables
```bash
EBMS_MSH_URL=https://qaoffentlig.meldingsformidler.digipost.no/api/
MQGATEWAY04_HOSTNAME=localhost
MQGATEWAY04_PORT=1414
MQGATEWAY04_NAME=mqgateway
SDPKANAL_CHANNEL_NAME=DEV.APP.SVRCONN
SDP_SEND_STANDARD_QUEUENAME=DEV.QUEUE.INPUT
SDP_SEND_PRIORITERT_QUEUENAME=DEV.QUEUE.INPUT_PRIORITY
SDP_SEND_PRIORITERT_BOQ_QUEUENAME=DEV.DEAD.LETTER.QUEUE
SDP_SEND_STANDARD_BOQ_QUEUENAME=DEV.DEAD.LETTER.QUEUE
SDP_KVITTERING_STANDARD_BOQ_QUEUENAME=DEV.DEAD.LETTER.QUEUE
SDP_KVITTERING_PRIORITERT_BOQ_QUEUENAME=DEV.DEAD.LETTER.QUEUE
SDP_KVITTERING_STANDARD_QUEUENAME=DEV.QUEUE.RECEIPT
SDP_KVITTERING_PRIORITERT_QUEUENAME=DEV.QUEUE.RECEIPT_PRIORITY
```

### Contact us
#### Code/project related questions can be sent to 
* Kevin Sillerud, `kevin.sillerud@nav.no`

#### For NAV employees
We're available on Slack in the channel #integrasjon for any inquiries.
