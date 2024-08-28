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

package contracts.positive.cases

import org.springframework.cloud.contract.spec.Contract

[
        Contract.make {
            name "Get all Sessions"
            description "when get all sessions, should return status 200"
            request {
                method GET()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
            }
            response {
                status OK()
                body([
                        "id"              : "PME-5797a5db-client-app-execution-0",
                        "sessionReference": "client-app-execution-0",
                        "duration"        : "18",
                        "pmeConfigId"     : "0",
                        "status"          : "CREATED"
                ])
            }
        },
        Contract.make {
            name "Get all Sessions when no Sessions exist for client"
            description "when get all sessions, and no sessions exist for client should return status 200 and an empty list"
            request {
                method GET()
                urlPath("/v1alpha3/sessions?clientId=dummy-app-id")
            }
            response {
                status OK()
                body([])
            }
        },
        Contract.make {
            name "Get all Sessions filtered by Session Reference"
            description "when get all sessions,and filter by a valid session reference, should return status 200"
            request {
                method GET()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4&sessionReference=client-app-execution-0")
            }
            response {
                status OK()
                body([
                        "id"              : "PME-5797a5db-client-app-execution-0",
                        "sessionReference": "client-app-execution-0",
                        "duration"        : "18",
                        "pmeConfigId"     : "0",
                        "status"          : "CREATED"
                ])
            }
        },
        Contract.make {
            name "Get all Sessions filtered by Session Reference when Session Reference does not exist"
            description "when get all sessions,and filter by a session reference that doesn't exist, should return status 200"
            request {
                method GET()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4&sessionReference=dummy-session-reference")
            }
            response {
                status OK()
                body([])
            }
        },
        Contract.make {
            name "Create new Session"
            description "when create session, should return status 201 & body"
            request {
                method POST()
                urlPath("/v1alpha3/sessions?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "sessionReference": "client-app-execution-0",
                        "pmeConfigId"     : "1"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status CREATED()
                body([
                        "id"              : "PME-5797a5db-client-app-execution-0",
                        "sessionReference": "client-app-execution-0",
                        "duration"        : "18",
                        "pmeConfigId"     : "1",
                        "status"          : "CREATED"
                ])
            }
        },
        Contract.make {
            name "Get Session by ID"
            description "when get session for ID, and the ID is valid, should return status 200"
            request {
                method GET()
                urlPath("/v1alpha3/sessions/PME-5797a5db-client-app-execution-0?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
            }
            response {
                status OK()
                body([
                        "id"              : "PME-5797a5db-client-app-execution-0",
                        "sessionReference": "client-app-execution-0",
                        "duration"        : "18",
                        "pmeConfigId"     : "0",
                        "status"          : "CREATED"
                ])
            }
        },
        Contract.make {
            name "Stop Session by ID"
            description "when stop session for ID, should return status 202"
            request {
                method PUT()
                urlPath("/v1alpha3/sessions/PME-5797a5db-client-app-execution-0/status?clientId=rappid--1--261854e7-2b26-4dc7-8d68-5bf01cfe62b4")
                body([
                        "status": "STOPPED"
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status ACCEPTED()
                body([
                        "status": "STOPPED"
                ])
            }
        },
]