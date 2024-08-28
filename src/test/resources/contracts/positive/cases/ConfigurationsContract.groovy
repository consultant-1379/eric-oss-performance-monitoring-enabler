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
            name "Get all Configurations"
            description "when get all configurations, should return status 200"
            request {
                method GET()
                urlPath("/v1alpha3/configurations")
            }
            response {
                status OK()
                body([])
            }
        },
        Contract.make {
            name "Get a Configuration by id"
            description "when get a configuration, should return status 200"
            request {
                method GET()
                urlPath("/v1alpha3/configurations/1")
            }
            response {
                status OK()
            }
        },
        Contract.make {
            name "Create a new Configuration"
            description "when create configuration, should return status 201"
            request {
                method POST()
                urlPath("/v1alpha3/configurations")
                body([
                        "name"              : "sample_configuration_with_all_predefined_kpis_included",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.9
                                ],
                                [
                                        "kpiName"       : "diff_initial_erab_establishment_sr_enodeb_hourly",
                                        "fixedThreshold": 98.90
                                ],
                                [
                                        "kpiName"       : "scg_active_radio_resource_retainability_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "avg_dl_mac_drb_throughput_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "avg_ul_mac_ue_throughput_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "normalized_avg_dl_mac_cell_throughput_traffic_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "endc_ps_cell_change_success_rate_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "partial_cell_availability_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "nr_to_lte_inter_rat_handover_sr_gnodeb_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "nr_handover_success_rate_gnodeb_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "cell_availability_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "initial_and_added_e_rab_establishment_sr_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "avg_ul_pdcp_ue_throughput_cell_hourly",
                                        "fixedThreshold": 100000
                                ],
                                [
                                        "kpiName"       : "avg_dl_pdcp_ue_throughput_cell_hourly",
                                        "fixedThreshold": 100000
                                ],
                                [
                                        "kpiName"       : "ul_pusch_sinr_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "cell_handover_success_rate_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "e_rab_retainability_percentage_lost_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "avg_dl_latency_hourly",
                                        "fixedThreshold": 1000
                                ],
                                [
                                        "kpiName"       : "voip_cell_integrity_hourly",
                                        "fixedThreshold": 99.00
                                ]
                        ]
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status CREATED()
                body([
                        "id"                : "0",
                        "name"              : "sample_configuration_with_all_predefined_kpis_included",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.9
                                ],
                                [
                                        "kpiName"       : "diff_initial_erab_establishment_sr_enodeb_hourly",
                                        "fixedThreshold": 98.9
                                ],
                                [
                                        "kpiName"       : "scg_active_radio_resource_retainability_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "avg_dl_mac_drb_throughput_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "avg_ul_mac_ue_throughput_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "normalized_avg_dl_mac_cell_throughput_traffic_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "endc_ps_cell_change_success_rate_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "partial_cell_availability_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "nr_to_lte_inter_rat_handover_sr_gnodeb_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "nr_handover_success_rate_gnodeb_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "cell_availability_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "initial_and_added_e_rab_establishment_sr_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "avg_ul_pdcp_ue_throughput_cell_hourly",
                                        "fixedThreshold": 100000
                                ],
                                [
                                        "kpiName"       : "avg_dl_pdcp_ue_throughput_cell_hourly",
                                        "fixedThreshold": 100000
                                ],
                                [
                                        "kpiName"       : "ul_pusch_sinr_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "cell_handover_success_rate_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "e_rab_retainability_percentage_lost_hourly",
                                        "fixedThreshold": 99
                                ],
                                [
                                        "kpiName"       : "avg_dl_latency_hourly",
                                        "fixedThreshold": 1000
                                ],
                                [
                                        "kpiName"       : "voip_cell_integrity_hourly",
                                        "fixedThreshold": 99
                                ]
                        ]
                ])
            }
        },
        Contract.make {
            name "Update an existing Configuration"
            description "when update configuration, should return status 200"
            request {
                method PUT()
                urlPath("/v1alpha3/configurations/1")
                body([
                        "name"              : "sample_configuration_with_all_predefined_kpis_included",
                        "weekendDays"       : "SATURDAY,SUNDAY",
                        "fixedThresholdKpis": [
                                [
                                        "kpiName"       : "en_dc_setup_sr_captured_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "diff_initial_erab_establishment_sr_enodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "scg_active_radio_resource_retainability_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "avg_dl_mac_drb_throughput_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "avg_ul_mac_ue_throughput_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "normalized_avg_dl_mac_cell_throughput_traffic_hourly",
                                        "fixedThreshold": 1000000
                                ],
                                [
                                        "kpiName"       : "endc_ps_cell_change_success_rate_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "partial_cell_availability_gnodeb_hourly",
                                        "fixedThreshold": 98.99
                                ],
                                [
                                        "kpiName"       : "nr_to_lte_inter_rat_handover_sr_gnodeb_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "nr_handover_success_rate_gnodeb_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "cell_availability_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "initial_and_added_e_rab_establishment_sr_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "avg_ul_pdcp_ue_throughput_cell_hourly",
                                        "fixedThreshold": 100000
                                ],
                                [
                                        "kpiName"       : "avg_dl_pdcp_ue_throughput_cell_hourly",
                                        "fixedThreshold": 100000
                                ],
                                [
                                        "kpiName"       : "ul_pusch_sinr_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "cell_handover_success_rate_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "e_rab_retainability_percentage_lost_hourly",
                                        "fixedThreshold": 99.00
                                ],
                                [
                                        "kpiName"       : "avg_dl_latency_hourly",
                                        "fixedThreshold": 1000
                                ],
                                [
                                        "kpiName"       : "voip_cell_integrity_hourly",
                                        "fixedThreshold": 99.00
                                ]
                        ]
                ])
                headers {
                    contentType applicationJson()
                }
            }
            response {
                status OK()
            }
        },
        Contract.make {
            name "Delete a Configuration"
            description "when delete a configuration, should return status 204"
            request {
                method DELETE()
                urlPath("/v1alpha3/configurations/1")
            }
            response {
                status NO_CONTENT()
            }
        }

]