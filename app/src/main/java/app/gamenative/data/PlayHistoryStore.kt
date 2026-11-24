package app.gamenative.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** When each library game was last played, by app id, kept where the home screen can read it for its "Recent" shelf. */
object PlayHistoryStore {
    private val _lastPlayed = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastPlayed: StateFlow<Map<String, Long>> = _lastPlayed.asStateFlow()

    fun update(history: Map<String, Long>) {
        _lastPlayed.value = history
    }
}
