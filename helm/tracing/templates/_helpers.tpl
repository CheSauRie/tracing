{{- define "tracing.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "tracing.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "tracing.fullname" -}}
{{- $context := .context -}}
{{- $name := .name -}}
{{- $app := index $context.Values.apps $name -}}
{{- if $app.fullnameOverride -}}
{{- $app.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $fullname := printf "%s-%s" (include "tracing.name" $context) $name -}}
{{- $fullname | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "tracing.labels" -}}
app.kubernetes.io/name: {{ .name }}
helm.sh/chart: {{ include "tracing.chart" .context }}
app.kubernetes.io/instance: {{ .context.Release.Name }}
app.kubernetes.io/managed-by: {{ .context.Release.Service }}
app.kubernetes.io/component: {{ default .name .component }}
{{- with .extra }}
{{ toYaml . | trimSuffix "\n" }}
{{- end }}
{{- end -}}
