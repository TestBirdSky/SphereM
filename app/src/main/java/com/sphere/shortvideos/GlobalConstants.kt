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

    /** 打开 MainActivity 后切换到提现 Tab（Wallet / [WithdrawFragment]） */
    const val EXTRA_KEY_OPEN_WALLET = "EXTRA_KEY_OPEN_WALLET"

    const val PRIVACY_POLICY = "https://www.google.com" // todo

    const val RISK_URL = "https://ip-prod.dramasphere.net/api/cbird"


    // todo check
    val NEW_DEFAULT_AD_LOCAL_JSON = /*if (isDebugMode) LOCAL_TEST_AD_DEFAULT_CON else*/ """
        {
          "dlmsf_switch": false,
          "fail_times": 100,
          "request_interval": 1,
          "dlmsf_req_s":false,
          "dlmsf_launch":[
              {
                 "byfxjhld":"ca-app-pub-3940256099942544/9257395921",
                 "dmhytwql":"admob",
                 "ugebepat":"open",
                 "gqqvwedz":13800,
                 "wdzqbsbt":3
              }
           ],
          "dlmsf_int": [
            {
              "byfxjhld": "ca-app-pub-3940256099942544/1033173712",
              "dmhytwql": "admob",
              "ugebepat": "interstitial",
              "gqqvwedz": 13800,
              "wdzqbsbt": 1
            }
          ],
          "dlmsf_rv": [
            {
              "byfxjhld": "ca-app-pub-3940256099942544/5224354917",
              "dmhytwql": "admob",
              "ugebepat": "reward",
              "gqqvwedz": 3000,
              "wdzqbsbt": 1
            }
          ]
        }
    """.trimIndent()

    //    val DEFAULT_JSON = """
    //        {
    //           "ds_launch":[
    //              {
    //                 "dsid":"ca-app-pub-3940256099942544/9257395921",
    //                 "amtt":"admob",
    //                 "dsty":"op",
    //                 "dsad":13800,
    //                 "dsei":3
    //              }
    //           ],
    //           "ds_unlock_int":[
    //              {
    //                 "dsid":"ca-app-pub-3940256099942544/1033173712",
    //                 "amtt":"admob",
    //                 "dsty":"int",
    //                 "dsad":3000,
    //                 "dsei":3
    //              }
    //           ]
    //        }
    //    """.trimIndent()

    const val LOCAL_TEST_AD_DEFAULT_CON = """
{
  "dlmsf_switch": false,
  "fail_times": 100,
  "request_interval": 1,
  "dlmsf_req_s": false,
  "dlmsf_sencebid": true,
  "dlmsf_launch": [
    {
      "byfxjhld": "n69cb667213998",
      "dmhytwql": "topon",
      "ugebepat": "interstitial",
      "gqqvwedz": 3000,
      "wdzqbsbt": 1
    }
  ],
  "dlmsf_int": [
    {
      "byfxjhld": "n69cb67522c47d",
      "dmhytwql": "topon",
      "ugebepat": "interstitial",
      "gqqvwedz": 3000,
      "wdzqbsbt": 1
    }
  ],
  "dlmsf_rv": [
    {
      "byfxjhld": "n69cb673cc2c72",
      "dmhytwql": "topon",
      "ugebepat": "reward",
      "gqqvwedz": 3000,
      "wdzqbsbt": 1
    }
  ],
  "dlmsf_scene_bid": {
 "dlmsf_launch": [
      {
        "byfxjhld": "983207411",
        "dmhytwql": "pangle",
        "ugebepat": "interstitial",
        "gqqvwedz": 3000,
        "wdzqbsbt": 1
      }
    ],
    "dlmsf_int": [
      {
        "byfxjhld": "983205646",
        "dmhytwql": "pangle",
        "ugebepat": "interstitial",
        "gqqvwedz": 3000,
        "wdzqbsbt": 1
      }
    ],
    "dlmsf_rv": [
      {
        "byfxjhld": "983205618",
        "dmhytwql": "pangle",
        "ugebepat": "reward",
        "gqqvwedz": 3000,
        "wdzqbsbt": 1
      }
    ]
  }
}        
"""
}