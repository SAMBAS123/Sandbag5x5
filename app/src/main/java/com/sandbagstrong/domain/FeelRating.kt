package com.sandbagstrong.domain

enum class FeelRating(val display: String) {
    EASY("Easy"),
    SOLID("Solid"),
    BRUTAL("Brutal"),
    FAILED("Failed");

    companion object {
        fun fromString(value: String): FeelRating =
            values().find { it.name == value || it.display.equals(value, ignoreCase = true) } ?: SOLID
    }
}
