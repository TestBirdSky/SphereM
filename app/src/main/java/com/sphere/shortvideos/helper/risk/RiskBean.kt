package com.sphere.shortvideos.helper.risk

/**
 * Date：2026/1/29
 * Describe:
 */
const val DEFAULT_RISK_BEAN = """
{
  "ui": {
    "number": 0,
    "behavior": 0,
    "device": 0
  },
  "behavior": {
    "ad_short_show": {
      "duration": 30,
      "value": 3
    },
    "ad_short_close": {
      "duration": 20,
      "value": 3
    },
    "wrong_deem_ad_less": 3,
    "wrong_deem_ad_more": 90,
    "no_install": 1,
    "ad_daily_show": 60
  },
  "device": [
    "vpn",
    "root",
    "sim",
    "simulator",
    "googleplay",
    "developer",
    "ip"
  ]
}
"""