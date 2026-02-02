package com.sphere.shortvideos.helper.reward

/**
 * Date：2026/1/23
 * Describe:
 */
const val DEFAULT_JSON = """
{
  "money_newuser_gift": {
    "reward": 100
  },
  "money_video_icon": [
    { "min": 100, "max": 110, "reward": [2, 3] },
    { "min": 110, "max": 130, "reward": [1.3, 2.5] },
    { "min": 130, "max": 150, "reward": [1, 1.7] },
    { "min": 150, "max": 210, "reward": [0.5, 1.5] },
    { "min": 210, "max": 225, "reward": [0.1, 0.7] },
    { "min": 225, "max": 230, "reward": [0.05, 0.2] },
    { "min": 230, "max": 235, "reward": [0.05, 0.08] },
    { "min": 235, "max": 237, "reward": [0.04, 0.06] },
    { "min": 237, "max": 239, "reward": [0.02, 0.04] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "money_push": [
    { "min": 100, "max": 110, "reward": [10] },
    { "min": 110, "max": 130, "reward": [8] },
    { "min": 130, "max": 150, "reward": [5] },
    { "min": 150, "max": 210, "reward": [3] },
    { "min": 210, "max": 225, "reward": [2] },
    { "min": 225, "max": 230, "reward": [1] },
    { "min": 230, "max": 235, "reward": [0.5] },
    { "min": 235, "max": 237, "reward": [0.3] },
    { "min": 237, "max": 239, "reward": [0.05] },
    { "min": 239, "max": null, "reward": [0.02] }
  ],
  "drama_time_1": [
    { "min": 100, "max": 110, "reward": [4, 5] },
    { "min": 110, "max": 130, "reward": [2, 3] },
    { "min": 130, "max": 150, "reward": [0.5, 1] },
    { "min": 150, "max": 210, "reward": [0.1, 0.3] },
    { "min": 210, "max": 225, "reward": [0.05, 0.1] },
    { "min": 225, "max": 230, "reward": [0.02, 0.05] },
    { "min": 230, "max": 235, "reward": [0.02, 0.03] },
    { "min": 235, "max": 237, "reward": [0.01, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.02] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "drama_time_2": [
    { "min": 100, "max": 110, "reward": [4, 5] },
    { "min": 110, "max": 130, "reward": [2, 3] },
    { "min": 130, "max": 150, "reward": [0.5, 1] },
    { "min": 150, "max": 210, "reward": [0.1, 0.3] },
    { "min": 210, "max": 225, "reward": [0.05, 0.1] },
    { "min": 225, "max": 230, "reward": [0.02, 0.05] },
    { "min": 230, "max": 235, "reward": [0.02, 0.03] },
    { "min": 235, "max": 237, "reward": [0.01, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.02] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "drama_time_3": [
    { "min": 100, "max": 110, "reward": [5.5, 6] },
    { "min": 110, "max": 130, "reward": [3.5, 4] },
    { "min": 130, "max": 150, "reward": [1.1, 1.5] },
    { "min": 150, "max": 210, "reward": [0.4, 0.6] },
    { "min": 210, "max": 225, "reward": [0.05, 0.1] },
    { "min": 225, "max": 230, "reward": [0.03, 0.05] },
    { "min": 230, "max": 235, "reward": [0.02, 0.03] },
    { "min": 235, "max": 237, "reward": [0.01, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.02] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "drama_time_4": [
    { "min": 100, "max": 110, "reward": [5.5, 6] },
    { "min": 110, "max": 130, "reward": [3.5, 4] },
    { "min": 130, "max": 150, "reward": [1.1, 1.5] },
    { "min": 150, "max": 210, "reward": [0.4, 0.6] },
    { "min": 210, "max": 225, "reward": [0.05, 0.1] },
    { "min": 225, "max": 230, "reward": [0.03, 0.05] },
    { "min": 230, "max": 235, "reward": [0.02, 0.03] },
    { "min": 235, "max": 237, "reward": [0.01, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.02] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "drama_time_5": [
    { "min": 100, "max": 110, "reward": [6.5, 8] },
    { "min": 110, "max": 130, "reward": [4.5, 5] },
    { "min": 130, "max": 150, "reward": [1.6, 2] },
    { "min": 150, "max": 210, "reward": [0.7, 0.8] },
    { "min": 210, "max": 225, "reward": [0.05, 0.1] },
    { "min": 225, "max": 230, "reward": [0.04, 0.05] },
    { "min": 230, "max": 235, "reward": [0.02, 0.03] },
    { "min": 235, "max": 237, "reward": [0.01, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.02] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "drama_time_6": [
    { "min": 100, "max": 110, "reward": [6.5, 8] },
    { "min": 110, "max": 130, "reward": [4.5, 5] },
    { "min": 130, "max": 150, "reward": [1.6, 2] },
    { "min": 150, "max": 210, "reward": [0.7, 0.8] },
    { "min": 210, "max": 225, "reward": [0.05, 0.1] },
    { "min": 225, "max": 230, "reward": [0.04, 0.05] },
    { "min": 230, "max": 235, "reward": [0.02, 0.03] },
    { "min": 235, "max": 237, "reward": [0.01, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.02] },
    { "min": 239, "max": null, "reward": [0.01, 0.02] }
  ],
  "task_pop": [
    { "min": 100, "max": 110, "reward": [1.5] },
    { "min": 110, "max": 130, "reward": [0.9] },
    { "min": 130, "max": 150, "reward": [0.6] },
    { "min": 150, "max": 210, "reward": [0.15] },
    { "min": 210, "max": 225, "reward": [0.1] },
    { "min": 225, "max": 230, "reward": [0.05] },
    { "min": 230, "max": 235, "reward": [0.02] },
    { "min": 235, "max": 237, "reward": [0.01] },
    { "min": 237, "max": 239, "reward": [0.01] },
    { "min": 239, "max": null, "reward": [0.01] }
  ],
  "sign_in": [
    { "min": 100, "max": 110, "reward": [5, 5, 10, 10, 15, 15, 20] },
    { "min": 110, "max": 130, "reward": [3, 3, 6, 6, 9, 9, 12] },
    { "min": 130, "max": 150, "reward": [2, 2, 4, 4, 6, 6, 10] },
    { "min": 150, "max": 210, "reward": [1, 1, 2, 2, 3, 3, 5] },
    { "min": 210, "max": 225, "reward": [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1] },
    { "min": 225, "max": 230, "reward": [0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05] },
    { "min": 230, "max": 235, "reward": [0.03, 0.03, 0.03, 0.03, 0.03, 0.03, 0.03] },
    { "min": 235, "max": 237, "reward": [0.02, 0.02, 0.02, 0.02, 0.02, 0.02, 0.02] },
    { "min": 237, "max": 239, "reward": [0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01] },
    { "min": 239, "max": null, "reward": [0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01] }
  ],
  "rv_video": [
    { "min": 100, "max": 110, "reward": [1.5] },
    { "min": 110, "max": 130, "reward": [0.9] },
    { "min": 130, "max": 150, "reward": [0.6] },
    { "min": 150, "max": 210, "reward": [0.3] },
    { "min": 210, "max": 225, "reward": [0.1] },
    { "min": 225, "max": 230, "reward": [0.05] },
    { "min": 230, "max": 235, "reward": [0.03] },
    { "min": 235, "max": 237, "reward": [0.02] },
    { "min": 237, "max": 239, "reward": [0.01] },
    { "min": 239, "max": null, "reward": [0.01] }
  ],
    "exit_reward": [
    { "min": 100, "max": 150, "reward": [2] },
    { "min": 150, "max": 180, "reward": [1.5] },
    { "min": 180, "max": 230, "reward": [1] },
    { "min": 230, "max": 239, "reward": [0.5] },
    { "min": 239, "max": null, "reward": [0.01] }
  ],
  "ad_interval": [2, 4, 2, 1]
}
"""