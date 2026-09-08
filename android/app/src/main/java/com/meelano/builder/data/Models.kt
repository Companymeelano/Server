package com.meelano.builder.data

data class Health(
    val ok: Boolean = false,
    val version: String = "",
    val ai: Boolean = false,
    val cloud_build: Boolean = false,
)

data class TemplateInfo(
    val id: String = "",
    val emoji: String = "",
    val name_en: String = "",
    val name_fa: String = "",
    val desc_en: String = "",
    val desc_fa: String = "",
    val accent: String = "#D9A7E6",
)

data class Suggestion(
    val emoji: String = "",
    val en: String = "",
    val fa: String = "",
)

data class TemplatesResp(
    val templates: List<TemplateInfo> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
)

data class CreateJobReq(
    val idea: String,
    val platforms: List<String>,
    val lang: String,
    val name: String = "",
    val ai_key: String = "",
    val ai_base: String = "",
)

data class Step(val name: String = "", val state: String = "")
data class Artifact(
    val kind: String = "",
    val label: String = "",
    val filename: String = "",
    val size: Long = 0,
    val url: String = "",
)

data class Job(
    val id: String = "",
    val idea: String = "",
    val platforms: List<String> = emptyList(),
    val lang: String = "",
    val name: String = "",
    val template: String = "",
    val slug: String = "",
    val status: String = "",
    val progress: Int = 0,
    val steps: List<Step> = emptyList(),
    val logs: List<String> = emptyList(),
    val artifacts: List<Artifact> = emptyList(),
    val error: String = "",
)

data class JobsResp(val jobs: List<Job> = emptyList())

fun Job.isTerminal() = status == "done" || status == "partial" || status == "failed"
