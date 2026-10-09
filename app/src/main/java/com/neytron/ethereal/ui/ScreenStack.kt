package com.neytron.ethereal.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Стек навигации по пространствам.
 *
 * Первый элемент — ROOT_ID, он неудаляем.
 * push(spaceId) — войти в пространство.
 * pop() — выйти на уровень выше.
 *
 * current — derivedStateOf, чтобы Compose отслеживал изменения и перекомпоновывал
 * экраны при push/pop.
 */
@Stable
class ScreenStack(initial: String = ROOT_ID) {

    val stack: SnapshotStateList<String> = mutableStateListOf(initial)

    val current: String by derivedStateOf {
        stack.lastOrNull() ?: ROOT_ID
    }

    val canGoBack: Boolean get() = stack.size > 1

    val depth: Int get() = stack.size

    fun push(spaceId: String) {
        if (spaceId == current) return
        stack.add(spaceId)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun resetToRoot() {
        while (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        }
    }

    companion object {
        const val ROOT_ID = "__root__"
    }
}