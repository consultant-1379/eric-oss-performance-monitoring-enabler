{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-data-generator.name" }}
  {{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart version as used by the chart label.
*/}}
{{- define "eric-oss-data-generator.version" }}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "eric-oss-data-generator.fullname" }}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- $name | trunc 63 | trimSuffix "-" }}
{{/* Ericsson mandates the name defined in metadata should start with chart name. */}}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-data-generator.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create image repo path
*/}}
{{- define "eric-oss-data-generator.repoPath" }}
  {{- if index .Values "imageCredentials" "eric-oss-data-generator" "repoPath" }}
    {{- index .Values "imageCredentials" "eric-oss-data-generator" "repoPath" }}
  {{- end }}
{{- end }}

{{/*
Create image registry url
*/}}
{{- define "eric-oss-data-generator.registryUrl" }}
  {{- $registryURL := "armdocker.rnd.ericsson.se" }}
  {{-  if .Values.global }}
    {{- if .Values.global.registry }}
      {{- if .Values.global.registry.url }}
        {{- $registryURL = .Values.global.registry.url }}
      {{- end }}
    {{- end }}
  {{- end }}
  {{- if index .Values "imageCredentials" "eric-oss-data-generator" "registry" }}
    {{- if index .Values "imageCredentials" "eric-oss-data-generator" "registry" "url" }}
      {{- $registryURL = index .Values "imageCredentials" "eric-oss-data-generator" "registry" "url" }}
    {{- end }}
  {{- end }}
  {{- print $registryURL }}
{{- end -}}

{{/*
Create image pull secrets
*/}}
{{- define "eric-oss-data-generator.pullSecrets" }}
  {{- $pullSecret := "" }}
  {{- if .Values.global }}
    {{- if .Values.global.pullSecret }}
      {{- $pullSecret = .Values.global.pullSecret }}
    {{- end }}
  {{- end }}
  {{- if index .Values "imageCredentials" "eric-oss-data-generator" }}
    {{- if index .Values "imageCredentials" "eric-oss-data-generator" "pullSecret" }}
      {{- $pullSecret = index .Values "imageCredentials" "eric-oss-data-generator" "pullSecret" }}
    {{- end }}
  {{- end }}
  {{- print $pullSecret }}
{{- end }}

{{/*
Timezone variable
*/}}
{{- define "eric-oss-data-generator.timezone" }}
  {{- $timezone := "UTC" }}
  {{- if .Values.global }}
    {{- if .Values.global.timezone }}
      {{- $timezone = .Values.global.timezone }}
    {{- end }}
  {{- end }}
  {{- print $timezone | quote }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-oss-data-generator.labels" }}
app.kubernetes.io/name: {{ include "eric-oss-data-generator.name" . }}
helm.sh/chart: {{ include "eric-oss-data-generator.chart" . }}
{{ include "eric-oss-data-generator.selectorLabels" . }}
app.kubernetes.io/version: {{ include "eric-oss-data-generator.version" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-oss-data-generator.fsGroup.coordinated" -}}
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
Selector labels
*/}}
{{- define "eric-oss-data-generator.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-data-generator.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-oss-data-generator.serviceAccountName" -}}
  {{- if .Values.serviceAccount.create }}
    {{- default (include "eric-oss-data-generator.fullname" .) .Values.serviceAccount.name }}
  {{- else }}
    {{- default "default" .Values.serviceAccount.name }}
  {{- end }}
{{- end }}

{{/*
Create a user defined annotation
*/}}
{{- define "eric-oss-data-generator.config-annotations" }}
  {{- if .Values.annotations -}}
    {{- range $name, $config := .Values.annotations }}
      {{ $name }}: {{ tpl $config $ }}
    {{- end }}
  {{- end }}
{{- end}}

{{/*
TODO: Please change this product number to a valid one, once it is available.
*/}}
{{- define "eric-oss-data-generator.product-info" }}
ericsson.com/product-name: "Testing tool for PM Stats Calc Handling"
ericsson.com/product-number: ""
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end }}

{{/*
Define the role reference for security policy
*/}}
{{- define "eric-oss-data-generator.securityPolicy.reference" -}}
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
{{- define "eric-oss-data-generator.securityPolicy.annotations" -}}
# Automatically generated annotations for documentation purposes.
{{- end -}}

Define Pod Disruption Budget value taking into account its type (int or string)
*/}}
{{- define "eric-oss-data-generator.pod-disruption-budget" -}}
  {{- if kindIs "string" .Values.podDisruptionBudget.minAvailable -}}
    {{- print .Values.podDisruptionBudget.minAvailable | quote -}}
  {{- else -}}
    {{- print .Values.podDisruptionBudget.minAvailable | atoi -}}
  {{- end -}}
{{- end -}}
