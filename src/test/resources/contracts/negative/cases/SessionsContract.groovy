/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package contracts.negative.cases

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            name "Create Session for no client ID"
            description "when create session for no client ID, should return 400"
            request {
                method POST()
                urlPath("/v1alpha3/sessions")
                body([
                        "sessionReference": "sample-rapp-execution-1",
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "400",
                        "title" : "Bad Request",
                        "detail": "Required parameter 'clientId' is not present."
                ])
            }
        },
        Contract.make {
            name "Create Session for invalid client ID"
            description "when create session for invalid client ID, should return 400"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=test id")
                body([
                        "sessionReference": "sample-rapp-execution-1",
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "400",
                        "title" : "Validation Failed",
                        "detail": "Client ID must contain only alphanumeric, underscore or dash characters"
                ])
            }
        },
        Contract.make {
            name "Create Conflicting Session"
            description "when create session for existing client & execution ID, should return 409 Conflict"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "sample-rapp-execution-0",
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status CONFLICT()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "409",
                        "title" : "Validation Failed",
                        "detail": "Session already exists for Client ID and Session reference"
                ])
            }
        },
        Contract.make {
            name "Create Invalid Session without PME config"
            description "when create session without PME Config ID, should return 400 Bad Request"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "sample-rapp-execution-0"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
                body([
                        "status": 400,
                        "title" : "Validation Failed",
                        "detail": "PME Config ID cannot be null"
                ])
            }
        },
        Contract.make {
            name "Create Invalid Session with negative duration"
            description "when create session with negative duration, should return 400 Bad Request"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "sample-rapp-execution-0",
                        "pmeConfigId"     : "1",
                        "duration"        : "0"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
                body([
                        "status": 400,
                        "title" : "Validation Failed",
                        "detail": "Session duration must be between 1 - 24 hours"
                ])
            }
        },
        Contract.make {
            name "Create Invalid Session with duration exceeding 24 hours"
            description "when create session with duration exceeding 24 hours, should return 400 Bad Request"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "sample-rapp-execution-0",
                        "pmeConfigId"     : "1",
                        "duration"        : "25"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
                body([
                        "status": 400,
                        "title" : "Validation Failed",
                        "detail": "Session duration must be between 1 - 24 hours",
                ])
            }
        },
        Contract.make {
            name "Create Invalid Session with empty Client Execution ID"
            description "when create session with empty Client Execution ID, should return 400 Bad Request"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "pmeConfigId": "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
                body([
                        "status": 400,
                        "title" : "Validation Failed",
                        "detail": "Session reference cannot be null"
                ])
            }
        },
        Contract.make {
            name "Create Invalid Session with invalid Client Execution ID"
            description "when create session with invalid Client Execution ID, should return 400 Bad Request"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "sample rapp execution 0",
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
                body([
                        "status": 400,
                        "title" : "Validation Failed",
                        "detail": "Session reference must contain only alphanumeric, underscore or dash characters"
                ])
            }
        },
        Contract.make {
            name "Create Invalid Session with Client Execution ID exceeding max length"
            description "when create session with Client Execution ID exceeding max length, should return 400 Bad Request"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "X".repeat(256),
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status BAD_REQUEST()
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
                body([
                        "status": 400,
                        "title" : "Validation Failed",
                        "detail": "Session reference must only have length of 4 - 64 characters"
                ])
            }
        },
        Contract.make {
            name "Create Session with invalid content type"
            description "when get session with an invalid content type, should return status 415 with error description"
            request {
                method POST()
                urlPath("/v1alpha3/sessions")
                body([
                        "sessionReference": "sample-rapp-execution-1",
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType textPlain()
                }
            }
            response {
                status UNSUPPORTED_MEDIA_TYPE()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "415",
                        "title" : "Validation Failed",
                        "detail": "Content-Type 'text/plain;charset=ISO-8859-1' is not supported."
                ])
            }
        },
        Contract.make {
            name "Get Session by ID when ID does not exist"
            description "when get session for ID, and the ID is invalid, should return status 404 with error description"
            request {
                method GET()
                urlPath("/v1alpha3/sessions/PME-5797a5db-client-app-execution-0?clientId=dummy-client-id")
            }
            response {
                status NOT_FOUND()
                body([
                        "status": 404,
                        "title" : "Provide a valid Client ID and Session ID",
                        "detail": "Session with provided ID does not exist"
                ])
            }
        },
        Contract.make {
            name "Stop session with invalid client ID"
            description "when stop session for ID, and the client ID does not exist, should return status 404 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/sessions/PME-5797a5db-client-app-execution-0/status?clientId=rappid--1--99999999-9999-9999-9999-999999999999")
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "STOPPED"
                ])
            }
            response {
                status NOT_FOUND()
                body([
                        "status": 404,
                        "title" : "Validation Failed",
                        "detail": "Session with provided ID does not exist"
                ])
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
            }
        },
        Contract.make {
            name "Stop session with a session ID that does not exist"
            description "when stop session for ID, and the session ID is invalid, should return status 404 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/sessions/dummy-id/status?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "STOPPED"
                ])
            }
            response {
                status NOT_FOUND()
                body([
                        "status": 404,
                        "title" : "Validation Failed",
                        "detail": "Session with provided ID does not exist"
                ])
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
            }
        },
        Contract.make {
            name "Stop session when session is already stopped"
            description "when stop session for ID, and the session is already stopped, should return status 409 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/sessions/PME-5797a5db-client-app-execution-0/status?clientId=rappid--1--261854e7-2b26-4dc7-8d68-99999")
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "STOPPED"
                ])
            }
            response {
                status CONFLICT()
                body([
                        "status": 409,
                        "title" : "Validation Failed",
                        "detail": "Session is already stopped"
                ])
                headers {
                    header(contentType(), $(regex("application/.*json.*")))
                }
            }
        },
        Contract.make {
            name "Stop Session with invalid content type"
            description "when stop session with an invalid content type, should return status 415 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/sessions/PME-5797a5db-client-app-execution-0/status?clientId=rappid--1--261854e7-2b26-4dc7-8d68-99999")
                body([
                        "status": "STOPPED"
                ])
                headers {
                    contentType textPlain()
                }
            }
            response {
                status UNSUPPORTED_MEDIA_TYPE()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "415",
                        "title" : "Validation Failed",
                        "detail": "Content-Type 'text/plain;charset=ISO-8859-1' is not supported."
                ])
            }
        }
]