package com.deepconnect.vpn.ui

/**
 * Filters for the connection log.
 *
 * Native log levels run from 0 (error) to 3 (trace), so "Info+" keeps everything except
 * the verbose debug/trace chatter.
 */
enum class LogFilter(val label: String) {
    All("All"),
    InfoPlus("Info+"),
    Errors("Errors"),
    ;

    fun matches(level: Int): Boolean = when (this) {
        All -> true
        InfoPlus -> level <= 1
        Errors -> level == 0
    }
}
