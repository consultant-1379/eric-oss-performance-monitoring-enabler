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
            name "Get configuration that does not exist"
            description "When configuration does not exist, should return status 404 with error description"
            request {
                method GET()
                urlPath("/v1alpha3/configurations/1")
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Provide a valid Configuration ID",
                        "status": 404,
                        "detail": "Configuration with provided ID does not exist"
                ])
            }
        },
        Contract.make {
            name "Create Configuration where the name is already used"
            description "when create configuration, should return status 400"
            request {
                method POST()
                urlPath("/v1alpha3/configurations")
                body([
                        "name"              : "sample_configuration_with_name_already_used",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.9
                                ]
                        ]
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
                        "detail": "Configuration name is already used in an existing configuration"
                ])
            }
        },
        Contract.make {
            name "Create Configuration with invalid content type"
            description "when create configuration with an invalid content type, should return status 415 with error description"
            request {
                method POST()
                urlPath("/v1alpha3/configurations")
                body([
                        "name"              : "sample_configuration_with_name_already_used",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.9
                                ]
                        ]
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
            name "Create Configuration where an internal error occurs"
            description "when create configuration, should return status 500"
            request {
                method POST()
                urlPath("/v1alpha3/configurations")
                body([
                        "name"              : "sample_configuration_with_internal_error",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.9
                                ]
                        ]
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status INTERNAL_SERVER_ERROR()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "500",
                        "title" : "Internal Server Error",
                        "detail": "Failed to persist change to the database"
                ])
            }
        },
        Contract.make {
            name "Update an non-existing Configuration"
            description "when update for non-existing configuration, should return status 404"
            request {
                method PUT()
                urlPath("/v1alpha3/configurations/99999")
                body([
                        "name"              : "sample_configuration_with_one_kpi_included",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ]
                        ]
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status NOT_FOUND()
            }
        },
        Contract.make {
            name "Update configuration that is used by a session"
            description "When configuration is being used by a session, should return status 409 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/configurations/101")
                body([
                        "name"              : "sample_configuration_101",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ]
                        ]
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
                        "title" : "Only Configurations not associated with a Session can be updated",
                        "status": 409,
                        "detail": "Configuration with provided ID is used in a Session"
                ])
            }
        },
        Contract.make {
            name "Update Configuration where the payload is missing mandatory data"
            description "when update configuration is missing mandatory data, should return status 400"
            request {
                method PUT()
                urlPath("/v1alpha3/configurations/102")
                body([
                        "name"       : "sample_configuration_with_missing_fixedThresholdKpis",
                        "weekendDays": "SATURDAY,SUNDAY"
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
                        "detail": "The fixedThresholdKpis cannot be null or empty"
                ])
            }
        },
        Contract.make {
            name "Update Configuration with invalid content type"
            description "when update configuration with an invalid content type, should return status 415 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/configurations/103")
                body([
                        "name"       : "sample_configuration_with_missing_fixedThresholdKpis",
                        "weekendDays": "SATURDAY,SUNDAY"
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
            name "Update configuration when there is a data access exception"
            description "When a data access exception occurs, should return status 500 with error description"
            request {
                method PUT()
                urlPath("/v1alpha3/configurations/100")
                body([
                        "name"              : "sample_configuration_100",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ]
                        ]
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status INTERNAL_SERVER_ERROR()
                headers {
                    contentType applicationJson()
                }
                body([
                        "status": "500",
                        "title" : "Internal Server Error",
                        "detail": "Failed to update database"
                ])
            }
        },
        Contract.make {
            name "Delete configuration that does not exist"
            description "When configuration does not exist, should return status 404 with error description"
            request {
                method DELETE()
                urlPath("/v1alpha3/configurations/99")
            }
            response {
                status NOT_FOUND()
                body([
                        "title" : "Provide a valid Configuration ID",
                        "status": 404,
                        "detail": "Configuration with provided ID does not exist"
                ])
            }
        },
        Contract.make {
            name "Delete configuration that is used by a session"
            description "When configuration is being used by a session, should return status 409 with error description"
            request {
                method DELETE()
                urlPath("/v1alpha3/configurations/2")
            }
            response {
                status CONFLICT()
                headers {
                    contentType applicationJson()
                }
                body([
                        "title" : "Only Configurations not associated with a Session can be deleted",
                        "status": 409,
                        "detail": "Configuration with provided ID is used in a Session"
                ])
            }
        },
        Contract.make {
            name "Delete configuration when there is a data access exception"
            description "When a data access exception occurs, should return status 500 with error description"
            request {
                method DELETE()
                urlPath("/v1alpha3/configurations/3")
            }
            response {
                status INTERNAL_SERVER_ERROR()
                headers {
                    contentType applicationJson()
                }
                body([
                        "title" : "Internal Server Error",
                        "status": 500,
                        "detail": "Failed to delete from database"
                ])
            }
        }
]