#
# COPYRIGHT Ericsson 2023
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

# Building custom version of K6 binary to include newest xk6-kafka - See: https://eteamproject.internal.ericsson.com/browse/EPRD-190
FROM golang:1.20.5-alpine3.18 as extensionsBuild
WORKDIR /go/src/go.k6.io/xk6/

ENV K6_VERSION=v0.45.1

RUN apk update; apk add git

RUN go install go.k6.io/xk6/cmd/xk6@v0.9.2 && \
    xk6 build \
      --with github.com/mostafa/xk6-kafka@v0.21.1 \
      --with github.com/grafana/xk6-output-timescaledb@v0.1.1 \
      --with github.com/grafana/xk6-sql@v0.3.0 \
      --with github.com/grafana/xk6-exec@v0.3.0

FROM armdocker.rnd.ericsson.se/proj-eric-oss-drop/k6-base-image:${k6BaseImageVersion}

COPY --from=extensionsBuild /go/src/go.k6.io/xk6/k6 /usr/local/bin/k6

# K6 VARIABLES
ENV APP_NAME=${name}
ENV APP_VERSION=${version}
ENV BUILD_URL="http://localhost/jenkins/job/eric-oss-performance-monitoring-enabler_PreCodeReview_Hybrid/1"
ENV ENVIRONMENT="development"
ENV OPTIONS_FILE="/resources/config/preOnboarding.options.json"
ENV PRODUCT="eric-oss-performance-monitoring-enabler"
ENV PRODUCT_VERSION=${version}
ENV SHUTDOWN_WAIT_TIME=0
ENV STAGING_TYPE="PRODUCT"
ENV TESTWARE_NAME=${name}
ENV TEST_VERSION=${version}
ENV DATABASE_URL=""
ENV K6_TOOL_GUI_URL=""
ENV API_URL=""
ENV ADDITIONAL_ARGS="--insecure-skip-tls-verify"
# EPME VARIABLES
ENV INGRESS_SCHEMA=""
ENV INGRESS_HOST=""
ENV INGRESS_LOGIN_USER=""
ENV INGRESS_LOGIN_PASSWORD=""
ENV EPME_INGRESS_SCHEMA=""
ENV EPME_INGRESS_HOST=""
ENV INGRESS_PME_TESTWARE_USER=""
ENV INGRESS_PME_TESTWARE_PASSWORD=""
ENV RAPP_ID=""
ENV BUILD_ID=""
ENV EPME_PREFIX=""
ENV EPME_DB_USER=""
ENV EPME_DB_PASSWORD=""
ENV EPME_DB_HOST=""
ENV EPME_DB_PORT=""
ENV EPME_DB_NAME=""
ENV KAFKA_TLS=""
ENV KAFKA_MONITORING_OBJECT_TOPIC=""

ADD js/ /tests
ADD resources/ /resources