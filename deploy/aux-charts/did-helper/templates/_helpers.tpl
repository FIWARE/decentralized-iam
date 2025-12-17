# did-helper/templates/_helpers.tpl
{{- define "did-helper.labels" -}}
helm.sh/chart: {{ include "did-helper.chart" . }}
{{ include "did-helper.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "did-helper.chart" -}}
{{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{- end }}

{{- define "did-helper.selectorLabels" -}}
app.kubernetes.io/name: did-helper
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}