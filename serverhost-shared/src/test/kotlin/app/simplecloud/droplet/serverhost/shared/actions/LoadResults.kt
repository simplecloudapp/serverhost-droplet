package app.simplecloud.droplet.serverhost.shared.actions

import app.simplecloud.droplet.serverhost.shared.actions.impl.copy.CopyActionData

object LoadResults {
    val FILE_LOAD_RESULT = listOf(
        YamlActionFile(fileName = "backup", listOf(YamlActionGroup("weekly", emptyList(), emptyList(), mapOf()))),
        YamlActionFile(
            fileName = "cache",
            listOf(
                YamlActionGroup("cache-pull", emptyList(), emptyList(), mapOf()),
                YamlActionGroup("cache-paper", emptyList(), emptyList(), mapOf()),
                YamlActionGroup("cache-spigot", emptyList(), emptyList(), mapOf())
            )
        )
    )

    private val GROUP_LOAD_RESULT_CACHE_SPIGOT = YamlActionGroup(
        "cache-spigot",
        listOf(
            YamlActionData(YamlActionTypes.COPY, CopyActionData("%server-dir%/libraries", "%templates%/cache/%group%/libraries")),
            YamlActionData(YamlActionTypes.COPY, CopyActionData("%server-dir%/versions", "%templates%/cache/%group%/versions"))
        ),
        emptyList(),
        mapOf(0 to Pair(YamlActionDataDescriptor.DATA, 0), 1 to Pair(YamlActionDataDescriptor.DATA, 1))
    )

    val GROUP_LOAD_RESULT = listOf(
        YamlActionFile(
            fileName = "backup",
            listOf(
                YamlActionGroup(
                    "weekly",
                    listOf(YamlActionData(YamlActionTypes.COPY, CopyActionData("", ""))),
                    emptyList(),
                    mapOf(0 to Pair(YamlActionDataDescriptor.DATA, 0))
                )
            )
        ),
        YamlActionFile(
            fileName = "cache",
            listOf(
                YamlActionGroup(
                    "cache-pull",
                    listOf(
                        YamlActionData(
                            YamlActionTypes.COPY,
                            CopyActionData("%templates%/cache/%group%", "%server-dir%")
                        )
                    ),
                    emptyList(),
                    mapOf(0 to Pair(YamlActionDataDescriptor.DATA, 0))
                ),
                YamlActionGroup(
                    "cache-paper",
                    listOf(
                        YamlActionData(
                            YamlActionTypes.COPY,
                            CopyActionData(
                                "%server-dir%/plugins/.paper-remapped/",
                                "%templates%/cache/%group%/plugins/.paper-remapped"
                            )
                        )
                    ),
                    listOf("cache/cache-spigot"),
                    mapOf(0 to Pair(YamlActionDataDescriptor.DATA, 0), 1 to Pair(YamlActionDataDescriptor.REF, 0))
                ),
                GROUP_LOAD_RESULT_CACHE_SPIGOT
            )
        )
    )
}