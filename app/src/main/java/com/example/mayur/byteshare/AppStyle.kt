package com.example.mayur.xportal

import io.multimoon.colorful.ThemeColor

object AppStyle {

    val PREFERENCE_NAME = "style"
    val THEME_COLOR_KEY = "theme_color_key"

    var currentThemeColor = Colors.GREY
    var currentTheme = ThemeColor.GREY

    object ColorValues {
        val DEEP_ORANGE = "deep_orange"
        val GREY = "grey"
        val GREEN = "green"
        val BLACK = "black"
        val LIGHT_BLUE = "light_blue"
        val PINK = "pink"
    }

    object Colors {
        val DEEP_ORANGE = "#ff8800"
        val GREY = "#808080"
        val GREEN = "#99cc00"
        val YELLOW = "#ffff00"
        val LIGHT_BLUE = "#33b5e5"
        val PINK = "#ffc0cb"
        val BLACK = "#000000"
    }
}
