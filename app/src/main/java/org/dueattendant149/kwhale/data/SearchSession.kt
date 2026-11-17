package org.dueattendant149.kwhale.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.dueattendant149.kwhale.network.model.SearchResponse

/**
 * In-process home for the search screen state.
 *
 * Tab switches and detail navigation dispose the SearchScreen composition, and
 * `remember` state died with it — going «назад» from an artist page landed on an
 * empty search screen instead of the previous results. The store lives for the
 * whole app process, so query, category and results survive navigation the way
 * users expect from modern apps. The screen binds with `by remember { session.x }`.
 */
class SearchSession {
    val query: MutableState<String> = mutableStateOf("")
    val selectedType: MutableState<String> = mutableStateOf("all")
    val results: MutableState<SearchResponse?> = mutableStateOf(null)
    /** Category the current `results` were fetched for (skip refetch on return). */
    val resultsType: MutableState<String> = mutableStateOf("all")

    fun clear() {
        query.value = ""
        selectedType.value = "all"
        results.value = null
        resultsType.value = "all"
    }
}
