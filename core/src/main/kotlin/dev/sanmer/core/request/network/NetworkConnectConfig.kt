package dev.sanmer.core.request.network

import dev.sanmer.core.request.container.ContainerConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkConnectConfig(
    @SerialName("Container")
    val container: String,
    @SerialName("EndpointConfig")
    val endpointConfig: ContainerConfig.Networking.EndpointSettings
)
