package com.example.mayur.byteshare.bloc.interfaces

abstract class Selectable {
    open var isSelected: Boolean = false
}

inline fun <reified T: Selectable> List<T>.selectAll() {
    for (item in this) item.isSelected = true
}

inline fun <reified T: Selectable> List<T>.unselectAll() {
    for (item in this) item.isSelected = false
}

inline fun <reified T: Selectable> List<T>.getAllSelected(): List<T> {
    return filter { it.isSelected }
}

inline fun <reified T: Selectable> List<T>.getAllUnselected(): List<T> {
    return filter { !it.isSelected }
}

inline fun <reified T: Selectable> List<T>.selectedCount(): Int {
    return count { it.isSelected }
}

inline fun <reified T: Selectable> List<T>.unselectedCount(): Int {
    return count { !it.isSelected }
}