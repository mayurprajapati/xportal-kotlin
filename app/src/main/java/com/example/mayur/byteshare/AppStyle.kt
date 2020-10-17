package com.example.mayur.byteshare

import io.multimoon.colorful.ThemeColor

object AppStyle {

    const val PREFERENCE_NAME = "style"
    const val THEME_COLOR_KEY = "theme_color_key"

    var currentThemeColor = Colors.GREY
    var currentTheme = ThemeColor.GREY

    object ColorValues {
        const val DEEP_ORANGE = "deep_orange"
        const val GREY = "grey"
        const val GREEN = "green"
        const val BLACK = "black"
        const val LIGHT_BLUE = "light_blue"
        const val PINK = "pink"
    }

    object Colors {
        const val DEEP_ORANGE = "#ff8800"
        const val GREY = "#808080"
        const val GREEN = "#99cc00"
        const val YELLOW = "#ffff00"
        const val LIGHT_BLUE = "#33b5e5"
        const val PINK = "#ffc0cb"
        const val BLACK = "#000000"
    }
}
