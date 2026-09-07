package com.clipboardmemory.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clipboardmemory.ClipboardApp
import com.clipboardmemory.data.ClipboardEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClipboardViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as ClipboardApp).database.clipboardDao()

    private val _entries = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    val entries: StateFlow<List<ClipboardEntry>> = _entries.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeEntries()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            if (_query.value.isBlank()) {
                dao.observeAll().collect { _entries.value = it }
            } else {
                dao.search(_query.value).collect { _entries.value = it }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val flow = if (newQuery.isBlank()) {
                dao.observeAll()
            } else {
                dao.search(newQuery)
            }
            flow.collect { _entries.value = it }
        }
    }

    fun stopSearch() {
        searchJob?.cancel()
        observeEntries()
    }

    fun clearAll() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }
}