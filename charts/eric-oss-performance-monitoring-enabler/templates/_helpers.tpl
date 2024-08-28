{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.fullname" -}}
{{- if .Values.fullnameOverride -}}
  {{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
  {{- $name := default .Chart.Name .Values.nameOverride -}}
  {{- printf "%s" $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Define the service id.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.serviceId" -}}
rapp-{{ include "eric-oss-performance-monitoring-enabler.name" . -}}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image pull secrets for global (outside of scope)
*/}}
{{- define "eric-oss-performance-monitoring-enabler.pullSecret.global" -}}
{{- $pullSecret := "" -}}
{{- if .Values.global -}}
  {{- if .Values.global.pullSecret -}}
    {{- $pullSecret = .Values.global.pullSecret -}}
  {{- end -}}
  {{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{/*
Create image pull secret, service level parameter takes precedence
*/}}
{{- define "eric-oss-performance-monitoring-enabler.pullSecret" -}}
{{- $pullSecret := (include "eric-oss-performance-monitoring-enabler.pullSecret.global" . ) -}}
{{- if .Values.imageCredentials -}}
  {{- if .Values.imageCredentials.pullSecret -}}
    {{- $pullSecret = .Values.imageCredentials.pullSecret -}}
  {{- end -}}
{{- end -}}
{{- print $pullSecret -}}
{{- end -}}

{{- define "eric-oss-performance-monitoring-enabler.mainImagePath" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := (index $productInfo "images" "eric-oss-performance-monitoring-enabler" "registry") -}}
    {{- $repoPath := (index $productInfo "images" "eric-oss-performance-monitoring-enabler" "repoPath") -}}
    {{- $name := (index $productInfo "images" "eric-oss-performance-monitoring-enabler" "name") -}}
    {{- $tag := (index $productInfo "images" "eric-oss-performance-monitoring-enabler" "tag") -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.url -}}
                {{- $registryUrl = .Values.global.registry.url -}}
            {{- end -}}
            {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
              {{- $repoPath = .Values.global.registry.repoPath -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if (index .Values "imageCredentials" "eric-oss-performance-monitoring-enabler") -}}
            {{- if (index .Values "imageCredentials" "eric-oss-performance-monitoring-enabler" "registry") -}}
                {{- if (index .Values "imageCredentials" "eric-oss-performance-monitoring-enabler" "registry" "url") -}}
                    {{- $registryUrl = (index .Values "imageCredentials" "eric-oss-performance-monitoring-enabler" "registry" "url") -}}
                {{- end -}}
            {{- end -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- $imagePath := printf "%s/%s/%s:%s" $registryUrl $repoPath $name $tag -}}
    {{- print (regexReplaceAll "[/]+" $imagePath "/") -}}
{{- end -}}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-performance-monitoring-enabler.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060)
*/}}
{{ define "eric-oss-performance-monitoring-enabler.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-oss-performance-monitoring-enabler.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
Merged labels for Default, which includes Standard and Config
*/}}
{{- define "eric-oss-performance-monitoring-enabler.labels" -}}
  {{- $standard := include "eric-oss-performance-monitoring-enabler.standard-labels" . | fromYaml -}}
  {{- $config := include "eric-oss-performance-monitoring-enabler.config-labels" . | fromYaml -}}
  {{- include "eric-oss-performance-monitoring-enabler.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) | trim }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-performance-monitoring-enabler.fsGroup.coordinated" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
      {{- if .Values.global.fsGroup.manual -}}
        {{ .Values.global.fsGroup.manual }}
      {{- else -}}
        {{- if eq .Values.global.fsGroup.namespace true -}}
          # The 'default' defined in the Security Policy will be used.
        {{- else -}}
          10000
      {{- end -}}
    {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
  {{- else -}}
    10000
  {{- end -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-performance-monitoring-enabler.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-performance-monitoring-enabler.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create the name of the postgres secret to use
*/}}
{{- define "eric-oss-performance-monitoring-enabler.pgSecretName" -}}
   {{- (include "eric-oss-performance-monitoring-enabler.fullname" .) }}-postgres-secret
{{- end }}

{{/*
Create container level annotations
*/}}
{{- define "eric-oss-performance-monitoring-enabler.container-annotations" }}
    {{- if .Values.appArmorProfile -}}
    {{- $appArmorValue := .Values.appArmorProfile.type -}}
        {{- if .Values.appArmorProfile.type -}}
            {{- if eq .Values.appArmorProfile.type "localhost" -}}
                {{- $appArmorValue = printf "%s/%s" .Values.appArmorProfile.type .Values.appArmorProfile.localhostProfile }}
            {{- end}}
container.apparmor.security.beta.kubernetes.io/eric-oss-performance-monitoring-enabler: {{ $appArmorValue | quote }}
        {{- end}}
    {{- end}}
{{- end}}

{{/*
Seccomp profile section (DR-1123-128)
*/}}
{{- define "eric-oss-performance-monitoring-enabler.seccomp-profile" }}
    {{- if .Values.seccompProfile }}
      {{- if .Values.seccompProfile.type }}
          {{- if eq .Values.seccompProfile.type "Localhost" }}
              {{- if .Values.seccompProfile.localhostProfile }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
  localhostProfile: {{ .Values.seccompProfile.localhostProfile }}
            {{- end }}
          {{- else }}
seccompProfile:
  type: {{ .Values.seccompProfile.type }}
          {{- end }}
        {{- end }}
      {{- end }}
{{- end }}

{{/*
Annotations for Product Name and Product Number (DR-D1121-064).
*/}}
{{- define "eric-oss-performance-monitoring-enabler.product-info" }}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060)
*/}}
{{ define "eric-oss-performance-monitoring-enabler.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-oss-performance-monitoring-enabler.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

Standard labels of Helm and Kubernetes
*/}}
{{- define "eric-oss-performance-monitoring-enabler.standard-labels" -}}
app.kubernetes.io/name: {{ include "eric-oss-performance-monitoring-enabler.name" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ include "eric-oss-performance-monitoring-enabler.version" . }}
helm.sh/chart: {{ include "eric-oss-performance-monitoring-enabler.chart" . }}
chart: {{ include "eric-oss-performance-monitoring-enabler.chart" . }}
{{- end -}}

{{/*
Merged annotations for Default, which includes productInfo, prometheus and config
*/}}
{{- define "eric-oss-performance-monitoring-enabler.annotations" -}}
  {{- $productInfo := include "eric-oss-performance-monitoring-enabler.product-info" . | fromYaml -}}
  {{- $prometheusAnn := include "eric-oss-performance-monitoring-enabler.prometheus" . | fromYaml -}}
  {{- $config := include "eric-oss-performance-monitoring-enabler.config-annotations" . | fromYaml -}}
  {{- include "eric-oss-performance-monitoring-enabler.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $prometheusAnn $config)) | trim }}
{{- end -}}

{{/*
Create prometheus info
*/}}
{{- define "eric-oss-performance-monitoring-enabler.prometheus" -}}
prometheus.io/path: {{ .Values.prometheus.path | quote }}
prometheus.io/port: {{ .Values.service.port | quote }}
prometheus.io/scrape: {{ .Values.prometheus.scrape | quote }}
{{- end -}}

{{/*
Create log control configmap name.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.log-control-configmap.name" }}
  {{- include "eric-oss-performance-monitoring-enabler.name" . | printf "%s-log-control-configmap" | quote }}
{{- end -}}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-performance-monitoring-enabler.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          default-restricted-security-policy
        {{- end -}}
      {{- else -}}
        default-restricted-security-policy
      {{- end -}}
    {{- else -}}
      default-restricted-security-policy
    {{- end -}}
  {{- else -}}
    default-restricted-security-policy
  {{- end -}}
{{- end -}}

{{/*
Define the annotations for security policy
*/}}
{{- define "eric-oss-performance-monitoring-enabler.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

{{/*
Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-performance-monitoring-enabler.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}

{{/*
Define upper limit for TerminationGracePeriodSeconds
*/}}
{{- define "eric-oss-performance-monitoring-enabler.terminationGracePeriodSeconds" -}}
{{- if .Values.terminationGracePeriodSeconds -}}
  {{- toYaml .Values.terminationGracePeriodSeconds -}}
{{- end -}}
{{- end -}}

{{/*
Define tolerations to comply to DR-D1120-060
*/}}
{{- define "eric-oss-performance-monitoring-enabler.tolerations" -}}
{{- if .Values.tolerations -}}
  {{- toYaml .Values.tolerations -}}
{{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-oss-performance-monitoring-enabler.nodeSelector" -}}
{{- $globalValue := (dict) -}}
{{- if .Values.global -}}
    {{- if .Values.global.nodeSelector -}}
      {{- $globalValue = .Values.global.nodeSelector -}}
    {{- end -}}
{{- end -}}
{{- if .Values.nodeSelector -}}
  {{- range $key, $localValue := .Values.nodeSelector -}}
    {{- if hasKey $globalValue $key -}}
         {{- $Value := index $globalValue $key -}}
         {{- if ne $Value $localValue -}}
           {{- printf "nodeSelector \"%s\" is specified in both global (%s: %s) and service level (%s: %s) with differing values which is not allowed." $key $key $globalValue $key $localValue | fail -}}
         {{- end -}}
     {{- end -}}
    {{- end -}}
    nodeSelector: {{- toYaml (merge $globalValue .Values.nodeSelector) | trim | nindent 2 -}}
{{- else -}}
  {{- if not ( empty $globalValue ) -}}
    nodeSelector: {{- toYaml $globalValue | trim | nindent 2 -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
    Define Image Pull Policy
*/}}
{{- define "eric-oss-performance-monitoring-enabler.registryImagePullPolicy" -}}
    {{- $globalRegistryPullPolicy := "IfNotPresent" -}}
    {{- if .Values.global -}}
        {{- if .Values.global.registry -}}
            {{- if .Values.global.registry.imagePullPolicy -}}
                {{- $globalRegistryPullPolicy = .Values.global.registry.imagePullPolicy -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- print $globalRegistryPullPolicy -}}
{{- end -}}



{/*
Define JVM heap size (DR-D1126-010 | DR-D1126-011)
*/}}
{{- define "eric-oss-performance-monitoring-enabler.jvmHeapSettings" -}}
    {{- $initRAM := "" -}}
    {{- $maxRAM := "" -}}
    {{/*
       ramLimit is set by default to 1.0, this is if the service is set to use anything less than M/Mi
       Rather than trying to cover each type of notation,
       if a user is using anything less than M/Mi then the assumption is its less than the cutoff of 1.3GB
       */}}
    {{- $ramLimit := 1.0 -}}
    {{- $ramComparison := 1.3 -}}

    {{- if not (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory") -}}
        {{- fail "memory limit for eric-oss-performance-monitoring-enabler is not specified" -}}
    {{- end -}}

    {{- if (hasSuffix "Gi" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "Gi" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "G" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory")) -}}
        {{- $ramLimit = trimSuffix "G" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory") | float64 -}}
    {{- else if (hasSuffix "Mi" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "Mi" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory") | float64) 1000) | float64  -}}
    {{- else if (hasSuffix "M" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory")) -}}
        {{- $ramLimit = (div (trimSuffix "M" (index .Values "resources" "eric-oss-performance-monitoring-enabler" "limits" "memory") | float64) 1000) | float64  -}}
    {{- end -}}

    {{- if (index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm") -}}
        {{- if (index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm" "initialMemoryAllocationPercentage") -}}
            {{- $initRAM = index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm" "initialMemoryAllocationPercentage" | float64 -}}
            {{- $initRAM = printf "-XX:InitialRAMPercentage=%f" $initRAM -}}
        {{- else -}}
            {{- fail "initialMemoryAllocationPercentage not set" -}}
        {{- end -}}
        {{- if and (index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm" "smallMemoryAllocationMaxPercentage") (index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm" "largeMemoryAllocationMaxPercentage") -}}
            {{- if lt $ramLimit $ramComparison -}}
                {{- $maxRAM =index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm" "smallMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- else -}}
                {{- $maxRAM = index .Values "resources" "eric-oss-performance-monitoring-enabler" "jvm" "largeMemoryAllocationMaxPercentage" | float64 -}}
                {{- $maxRAM = printf "-XX:MaxRAMPercentage=%f" $maxRAM -}}
            {{- end -}}
        {{- else -}}
            {{- fail "smallMemoryAllocationMaxPercentage | largeMemoryAllocationMaxPercentage not set" -}}
        {{- end -}}
    {{- else -}}
        {{- fail "jvm heap percentages are not set" -}}
    {{- end -}}
{{- printf "%s %s" $initRAM $maxRAM -}}
{{- end -}}

{{/*
Define the log streaming method parameter (DR-470222-010)
*/}}
{{- define "eric-oss-performance-monitoring-enabler.streamingMethod" -}}
{{- $streamingMethod := "direct" -}}
{{- if .Values.global -}}
  {{- if .Values.global.log -}}
      {{- if .Values.global.log.streamingMethod -}}
        {{- $streamingMethod = .Values.global.log.streamingMethod }}
      {{- end -}}
  {{- end -}}
{{- end -}}
{{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
    {{- $streamingMethod = .Values.log.streamingMethod }}
  {{- end -}}
{{- end -}}
{{- print $streamingMethod -}}
{{- end -}}

{{/*
Define the label needed for reaching eric-log-transformer (DR-470222-010)
*/}}
{{- define "eric-oss-performance-monitoring-enabler.directStreamingLabel" -}}
{{- $streamingMethod := (include "eric-oss-performance-monitoring-enabler.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) }}
logger-communication-type: "direct"
{{- end -}}
{{- end -}}

Define the label needed for scraping pm-metrics
*/}}
{{- define "eric-oss-performance-monitoring-enabler.pmMetricsScraping" -}}
service.cleartext/scraping: "true"
{{- end -}}

{{/*
Define logging environment variables and decide on expected behavior (DR-470222-010)
*/}}
{{ define "eric-oss-performance-monitoring-enabler.loggingEnv" }}
{{- $streamingMethod := (include "eric-oss-performance-monitoring-enabler.streamingMethod" .) -}}
{{- if or (eq "direct" $streamingMethod) (eq "dual" $streamingMethod) -}}
  {{- if eq "direct" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-https.xml"
- name: LOG_STREAMING_METHOD
  value: "direct"
  {{- end }}
  {{- if eq "dual" $streamingMethod }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-dual-sec.xml"
- name: LOG_STREAMING_METHOD
  value: "dual"
  {{- end }}
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: POD_UID
  valueFrom:
    fieldRef:
      fieldPath: metadata.uid
- name: CONTAINER_NAME
  value: eric-oss-performance-monitoring-enabler
- name: NODE_NAME
  valueFrom:
    fieldRef:
      fieldPath: spec.nodeName
- name: NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- else if eq $streamingMethod "indirect" }}
- name: LOGBACK_CONFIG_FILE
  value: "classpath:logback-json.xml"
- name: LOG_STREAMING_METHOD
  value: "indirect"
{{- else }}
  {{- fail ".log.streamingMethod unknown" }}
{{- end -}}
{{ end }}

{{/*
IAM URL
*/}}
{{ define "eric-oss-performance-monitoring-enabler.iamUrl" }}
  {{- $iamHost := .Values.baseUrl -}}
  {{- if ((((.Values).global).hosts).iam) -}}
    {{- $iamHost = ((((.Values).global).hosts).iam) -}}
  {{- end -}}
  {{- if (((.Values).iam).host) -}}
    {{- $iamHost = (((.Values).iam).host) -}}
  {{- end -}}
  {{ if hasPrefix "https://" $iamHost }}
    {{- printf "%s" $iamHost -}}
  {{ else }}
    {{- printf "https://%s" $iamHost -}}
  {{- end -}}
{{- end -}}

{{/*
PMSCH Hostname
*/}}
{{ define "eric-oss-performance-monitoring-enabler.pmschHost" }}
  {{- $pmschHost := "" -}}
  {{- if ((((.Values).global).hosts).gas) -}}
    {{- $pmschHost = ((((.Values).global).hosts).gas) -}}
  {{- end -}}
  {{- if (((.Values).pmsch).host) -}}
    {{- $pmschHost = (((.Values).pmsch).host) -}}
  {{- end -}}
  {{- printf "%s" $pmschHost -}}
{{- end -}}

{{/*
PMSCH URL
*/}}
{{ define "eric-oss-performance-monitoring-enabler.pmschUrl" }}
  {{- $pmschUrl := (include "eric-oss-performance-monitoring-enabler.pmschHost" . ) -}}
  {{- if (((.Values).pmsch).url) -}}
    {{- $pmschUrl = (((.Values).pmsch).url) -}}
  {{- end -}}
  {{ if (or (hasPrefix "https://" $pmschUrl) (hasPrefix "http://" $pmschUrl)) }}
    {{- printf "%s" $pmschUrl -}}
  {{ else }}
    {{- printf "https://%s" $pmschUrl -}}
  {{- end -}}
{{- end -}}

{{/*
DMM Hostname
*/}}
{{ define "eric-oss-performance-monitoring-enabler.dmmHost" }}
  {{- $dmmHost := "" -}}
  {{- if ((((.Values).global).hosts).gas) -}}
    {{- $dmmHost = ((((.Values).global).hosts).gas) -}}
  {{- end -}}
  {{- if (((.Values).dmm).host) -}}
    {{- $dmmHost = (((.Values).dmm).host) -}}
  {{- end -}}
  {{- printf "%s" $dmmHost -}}
{{- end -}}

{{/*
DMM URL
*/}}
{{ define "eric-oss-performance-monitoring-enabler.dmmUrl" }}
  {{- $dmmUrl := (include "eric-oss-performance-monitoring-enabler.dmmHost" . ) -}}
  {{- if (((.Values).dmm).url) -}}
    {{- $dmmUrl = (((.Values).dmm).url) -}}
  {{- end -}}
  {{ if (or (hasPrefix "https://" $dmmUrl) (hasPrefix "http://" $dmmUrl)) }}
    {{- printf "%s" $dmmUrl -}}
  {{ else }}
    {{- printf "https://%s" $dmmUrl -}}
  {{- end -}}
{{- end -}}

{{/*
TLS version
*/}}
{{ define "eric-oss-performance-monitoring-enabler.clientProtocol" }}
  {{- $clientProtocol := .Values.tls.clientProtocol -}}
  {{- printf "-Djdk.tls.client.protocols=%s" $clientProtocol -}}
{{- end -}}

{{/*
Label to access PME Database
*/}}
{{ define "eric-oss-performance-monitoring-enabler.pgAccessLabel" }}
  {{- $pmgPgValues := (index .Values "eric-oss-performance-monitoring-enabler-pg") -}}
  {{- if $pmgPgValues.enabled -}}
    {{- $pmePgName := "eric-oss-performance-monitoring-enabler-pg" -}}
    {{- if (($pmgPgValues).nameOverride) -}}
      {{- $pmePgName = $pmgPgValues.nameOverride -}}
    {{- end -}}
    {{ printf "%s-access" $pmePgName -}}: "true"
  {{- end -}}
{{- end -}}