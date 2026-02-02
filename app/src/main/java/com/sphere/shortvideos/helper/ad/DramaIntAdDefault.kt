package com.sphere.shortvideos.helper.ad

/**
 * 加密后的 JSON 字符串（使用 Base64 + 异或加密，密钥为 42）
 * 
 * 原始 JSON 字符串：
 * {
 *   "br_int_ad": [
 *     {
 *       "first_number": 0,
 *       "point": 0,
 *       "end_number": 100
 *     },
 *     {
 *       "first_number": 100,
 *       "point": 60,
 *       "end_number": 150
 *     },
 *     {
 *       "first_number": 150,
 *       "point": 80,
 *       "end_number": 200
 *     },
 *     {
 *       "first_number": 240,
 *       "point": 100,
 *       "end_number": null
 *     }
 *   ],
 *   "id_int_ad": [
 *     {
 *       "first_number": 0,
 *       "point": 0,
 *       "end_number": 300000
 *     },
 *     {
 *       "first_number": 300000,
 *       "point": 60,
 *       "end_number": 500000
 *     },
 *     {
 *       "first_number": 500000,
 *       "point": 80,
 *       "end_number": 700000
 *     },
 *     {
 *       "first_number": 700000,
 *       "point": 100,
 *       "end_number": null
 *     }
 *   ],
 *   "us_int_ad": [
 *     {
 *       "first_number": 0,
 *       "point": 0,
 *       "end_number": 20
 *     },
 *     {
 *       "first_number": 20,
 *       "point": 60,
 *       "end_number": 30
 *     },
 *     {
 *       "first_number": 30,
 *       "point": 80,
 *       "end_number": 40
 *     },
 *     {
 *       "first_number": 48,
 *       "point": 100,
 *       "end_number": null
 *     }
 *   ]
 * }
 */
const val DEFAULT_DRAMA_INT_AD_JSON = "USAKCghIWHVDRF51S04IEApxIAoKCgpRIAoKCgoKCghMQ1hZXnVEX0dIT1gIEAoaBiAKCgoKCgoIWkVDRF4IEAoaBiAKCgoKCgoIT0ROdURfR0hPWAgQChsaGiAKCgoKVwYgCgoKClEgCgoKCgoKCExDWFledURfR0hPWAgQChsaGgYgCgoKCgoKCFpFQ0ReCBAKHBoGIAoKCgoKCghPRE51RF9HSE9YCBAKGx8aIAoKCgpXBiAKCgoKUSAKCgoKCgoITENYWV51RF9HSE9YCBAKGx8aBiAKCgoKCgoIWkVDRF4IEAoSGgYgCgoKCgoKCE9ETnVEX0dIT1gIEAoYGhogCgoKClcGIAoKCgpRIAoKCgoKCghMQ1hZXnVEX0dIT1gIEAoYHhoGIAoKCgoKCghaRUNEXggQChsaGgYgCgoKCgoKCE9ETnVEX0dIT1gIEApEX0ZGIAoKCgpXIAoKdwYgCgoIQ051Q0RedUtOCBAKcSAKCgoKUSAKCgoKCgoITENYWV51RF9HSE9YCBAKGgYgCgoKCgoKCFpFQ0ReCBAKGgYgCgoKCgoKCE9ETnVEX0dIT1gIEAoZGhoaGhogCgoKClcGIAoKCgpRIAoKCgoKCghMQ1hZXnVEX0dIT1gIEAoZGhoaGhoGIAoKCgoKCghaRUNEXggQChwaBiAKCgoKCgoIT0ROdURfR0hPWAgQCh8aGhoaGiAKCgoKVwYgCgoKClEgCgoKCgoKCExDWFledURfR0hPWAgQCh8aGhoaGgYgCgoKCgoKCFpFQ0ReCBAKEhoGIAoKCgoKCghPRE51RF9HSE9YCBAKHRoaGhoaIAoKCgpXBiAKCgoKUSAKCgoKCgoITENYWV51RF9HSE9YCBAKHRoaGhoaBiAKCgoKCgoIWkVDRF4IEAobGhoGIAoKCgoKCghPRE51RF9HSE9YCBAKRF9GRiAKCgoKVyAKCncGIAoKCF9ZdUNEXnVLTggQCnEgCgoKClEgCgoKCgoKCExDWFledURfR0hPWAgQChoGIAoKCgoKCghaRUNEXggQChoGIAoKCgoKCghPRE51RF9HSE9YCBAKGBogCgoKClcGIAoKCgpRIAoKCgoKCghMQ1hZXnVEX0dIT1gIEAoYGgYgCgoKCgoKCFpFQ0ReCBAKHBoGIAoKCgoKCghPRE51RF9HSE9YCBAKGRogCgoKClcGIAoKCgpRIAoKCgoKCghMQ1hZXnVEX0dIT1gIEAoZGgYgCgoKCgoKCFpFQ0ReCBAKEhoGIAoKCgoKCghPRE51RF9HSE9YCBAKHhogCgoKClcGIAoKCgpRIAoKCgoKCghMQ1hZXnVEX0dIT1gIEAoeEgYgCgoKCgoKCFpFQ0ReCBAKGxoaBiAKCgoKCgoIT0ROdURfR0hPWAgQCkRfRkYgCgoKClcgCgp3IFc="
