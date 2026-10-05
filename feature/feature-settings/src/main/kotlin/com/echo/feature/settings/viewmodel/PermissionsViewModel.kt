package com.echo.feature.settings.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val folderAccess: FolderAccess,
) : ViewModel() {
    private val _folders = MutableStateFlow<List<FolderAccessRow>>(emptyList())
    val folders: StateFlow<List<FolderAccessRow>> = _folders.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _folders.value = folderAccess.rows() }
    }

    fun regrant(row: FolderAccessRow, uri: Uri) {
        viewModelScope.launch {
            _message.value = folderAccess.regrant(row, uri)
            _folders.value = folderAccess.rows()
        }
    }

    fun messageShown() = _message.update { null }
}
