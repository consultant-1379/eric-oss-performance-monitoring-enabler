# CSAR packaging Readme

## Run CSAR packaging locally

1. Run the following command with the arguments described below from the project root directory.
   ./localScripts/localPackageCsarScript.sh

### Arguments
There are optional 4 Arguments that can be used:
1. -c This will force delete the Temp Workdir (./tmp), Default is OFF
2. -u This will force update the Helm dependencies, Default is OFF
3. -m This will turn on running the maven build, Default is OFF
4. -v This will run with verbose, Default is Non-Verbose

## Results
1. eric-oss-performance-monitoring-enabler will be packaged to ./tmp/output/eric-oss-performance-monitoring-enabler-<SIGNUM>-<VERSION>.csar

When instantiating the name override is set to pme-<SIGNUM> by default. 
The PME endpoints for your instance will be /pme-<SIGNUM>

## CI/CD

This is **not** used as part of the CI Pipeline, and is to be used for local testing only. CSAR Packaging for auto-apps are handled by Auto-Apps product staging.

Some resources regarding CSAR generation in CI:
* [Auto Apps Test Strategy](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/IDUN/Auto+Apps+Test+Strategy#AutoAppsProductStaging)
* [EIAP Auto Apps Product Staging Pipeline](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/DGBase/EIAP+Auto+Apps+Product+Staging+Pipeline)
* [Job: EIAP-AUTO-APP-CSAR-Builder](https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/DGBase/Job%3A+EIAP-AUTO-APP-CSAR-Builder)

---
# Create IAM Client

### Arguments
There are optional 4 Arguments that can be used:
1. -h The hostname of IAM, Default is 'iam.stsvp1eic28.stsoss.sero.gic.ericsson.se'
2. -u The name of the IAM user, Default is 'kcadmin'
3. -p The password of the IAM user, Default is 'idunEr!css0n'
4. -c The client name, Default is 'epme_local_client'

---
# Upload CSAR File

### Arguments
Required:
1. -f The location of the CSAR file

There are optional 3 Arguments that can be used:
1. -h The hostname of AppMgr, Default is 'gas.stsvp1eic28.stsoss.sero.gic.ericsson.se'
2. -u The name of the appmgr user, Default is 'appmgr-user'
3. -p The password of the appmgr user, Default is 'idunEr!css0n'