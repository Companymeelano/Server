package com.meelano.builder.ui

object Brand {
    const val SHORT = "MeeLano"
    const val FULL = "MeeLano Builder"
    const val TEAM_EN = "Made with ❤️ by MeeLano Team"
    const val TEAM_FA = "ساخته شده با ❤️ توسط تیم میلانو"
    // Published by CI to releases/version.json (main branch).
    const val UPDATE_URL =
        "https://raw.githubusercontent.com/Companymeelano/Server/main/releases/version.json"
    val TEMPLATE_ACCENTS = mapOf(
        "blog" to "#D9A7E6",
        "calculator" to "#7BD88F",
        "notes" to "#F2D060",
        "dice" to "#FF9D6F",
        "weather" to "#6FC3FF",
        "shop" to "#FF7DAD",
        "starter" to "#D9A7E6",
    )
}
