package com.dndsaas.controller

/**
 * Header that identifies the calling user.
 *
 * A stand-in until real authentication exists: once login is added, the user
 * will come from the session/token instead, and only the controllers that read
 * this header need to change.
 */
const val USER_ID_HEADER = "X-User-Id"
