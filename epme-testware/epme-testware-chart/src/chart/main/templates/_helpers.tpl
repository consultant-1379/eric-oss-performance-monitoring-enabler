{{/*
Expand the name of the chart.
*/}}
{{- define "eric-oss-performance-monitoring-enabler-testware.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}


{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-oss-performance-monitoring-enabler-testware.chart" -}}
{{- printf "%s-%s" .Release.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}


{{/*
Common labels
*/}}
{{- define "eric-oss-performance-monitoring-enabler-testware.labels" -}}
helm.sh/chart: {{ include "eric-oss-performance-monitoring-enabler-testware.chart" . }}
{{ include "eric-oss-performance-monitoring-enabler-testware.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}


{{/*
Selector labels
*/}}
{{- define "eric-oss-performance-monitoring-enabler-testware.selectorLabels" -}}
app.kubernetes.io/name: {{ include "eric-oss-performance-monitoring-enabler-testware.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}


{{/*
Define kafka tls secret
*/}}
{{- define "eric-oss-performance-monitoring-enabler-testware.kafka-tls-secret" -}}
  {{- include "eric-oss-performance-monitoring-enabler-testware.name" . }}-kafka-secret
{{- end -}}