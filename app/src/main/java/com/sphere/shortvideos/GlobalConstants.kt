package com.sphere.shortvideos

object GlobalConstants {

    const val EXTRA_KEY_SHORT_PLAY = "EXTRA_KEY_SHORT_PLAY"
    const val EXTRA_KEY_START_INDEX = "EXTRA_KEY_START_INDEX"
    const val EXTRA_KEY_DRAMA_HISTORY = "EXTRA_KEY_DRAMA_HISTORY"
    const val EXTRA_KEY_START_PROGRESS = "EXTRA_KEY_START_PROGRESS"
    const val EXTRA_KEY_COMMON_BOOLEAN = "EXTRA_KEY_COMMON_BOOLEAN"
    const val EXTRA_KEY_COMMON_STRING = "EXTRA_KEY_COMMON_STRING"
    const val EXTRA_KEY_COMMON_INT = "EXTRA_KEY_COMMON_INT"
    const val EXTRA_KEY_COMMON_LONG = "EXTRA_KEY_COMMON_LONG"

    const val PRIVACY_POLICY = "https://www.google.com"

    val DEFAULT_JSON = """
        {
           "ds_launch":[
              {
                 "dsid":"ca-app-pub-3940256099942544/9257395921",
                 "amtt":"admob",
                 "dsty":"op",
                 "dsad":13800,
                 "dsei":3
              }
           ],
           "ds_unlock_int":[
              {
                 "dsid":"ca-app-pub-3940256099942544/1033173712",
                 "amtt":"admob",
                 "dsty":"int",
                 "dsad":3000,
                 "dsei":3
              }
           ]
        }
    """.trimIndent()

}