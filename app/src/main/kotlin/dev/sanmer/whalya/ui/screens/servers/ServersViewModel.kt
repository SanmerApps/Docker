package dev.sanmer.whalya.ui.screens.servers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sanmer.core.Docker.get
import dev.sanmer.core.resource.System
import dev.sanmer.core.response.system.SystemVersion
import dev.sanmer.whalya.Logger
import dev.sanmer.whalya.database.entity.ServerEntity
import dev.sanmer.whalya.model.LoadData
import dev.sanmer.whalya.repository.ClientRepository
import dev.sanmer.whalya.repository.DbRepository
import io.ktor.client.call.body
import kotlinx.coroutines.launch

class ServersViewModel(
    private val dbRepository: DbRepository,
    private val clientRepository: ClientRepository
) : ViewModel() {
    var data by mutableStateOf<LoadData<List<ServerEntity>>>(LoadData.Loading)
        private set

    var server by clientRepository

    private val pings = mutableStateMapOf<Long, Boolean>()

    private val logger = Logger.Android("ServersViewModel")

    init {
        logger.d("init")
        dbObserver()
    }

    fun ping(server: ServerEntity): Boolean {
        viewModelScope.launch {
            if (pings[server.id] != true) update(server)
        }
        return pings.getOrDefault(server.id, false)
    }

    private fun dbObserver() {
        viewModelScope.launch {
            dbRepository.getServersAsFlow()
                .collect { list ->
                    data = LoadData.Success(list.sortedBy { it.name })
                }
        }
    }

    private suspend fun update(server: ServerEntity) {
        runCatching {
            val version = clientRepository.getOrCreate(server)
                .get(System.Version())
                .body<SystemVersion>()

            pings[server.id] = true
            dbRepository.updateServer(
                server.copy(
                    version = version.version,
                    os = version.os,
                    arch = version.arch
                )
            )
        }.onFailure {
            pings[server.id] = false
            logger.e(it)
        }
    }
}