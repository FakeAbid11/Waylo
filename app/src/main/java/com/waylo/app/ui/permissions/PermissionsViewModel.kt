package com.waylo.app.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.waylo.app.core.permissions.PermissionManager
import com.waylo.app.core.permissions.PermissionStatus
import com.waylo.app.core.permissions.WayloPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PermissionsUiState(
    val statuses: List<PermissionStatus> = emptyList(),
)

class PermissionsViewModel(
    private val permissionManager: PermissionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(statuses = permissionManager.statuses()) }
    }

    fun permissionsToRequest(): List<WayloPermission> = permissionManager.permissionsToRequest()

    fun onRequestLaunched(permissions: List<WayloPermission>) {
        permissionManager.markRequested(permissions)
        refresh()
    }

    companion object {
        fun factory(permissionManager: PermissionManager): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { PermissionsViewModel(permissionManager) }
            }
    }
}
