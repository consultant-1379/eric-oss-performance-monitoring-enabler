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

ARG NODEJS_BUILDER_URL=armdocker.rnd.ericsson.se/proj-adp-cicd-drop/adp-nodejs-lts-builder-image
ARG NODEJS_BUILDER_VERSION=16.16.0-1

FROM $NODEJS_BUILDER_URL:$NODEJS_BUILDER_VERSION

RUN mkdir -p /k6
WORKDIR /k6

# Copy the npm project file first and install the dependencies
COPY epme-testware/epme-testware-impl/src/package.json package.json
RUN npm run install:all

# Now copy the remaining files and run lint over them
COPY epme-testware/epme-testware-impl/src/.eslintignore .eslintignore
COPY epme-testware/epme-testware-impl/src/.eslintrc.cjs .eslintrc.cjs
COPY epme-testware/epme-testware-impl/src/.prettierrc .prettierrc
COPY epme-testware/epme-testware-impl/src/main main

ENTRYPOINT [ "npm", "run", "lint" ]