package dev.sanmer.whalya.model.ui.inspect

import dev.sanmer.core.Labels
import dev.sanmer.core.request.container.ContainerConfig
import dev.sanmer.core.request.container.HostConfig
import dev.sanmer.core.response.container.Container
import dev.sanmer.core.response.container.ContainerLowLevel
import dev.sanmer.whalya.R
import dev.sanmer.whalya.compat.ContextCompat
import dev.sanmer.whalya.ktx.copy
import dev.sanmer.whalya.ktx.sizeBySI
import dev.sanmer.whalya.model.ui.home.UiContainer.Default.name
import dev.sanmer.whalya.model.ui.home.UiImage.Default.digest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

data class UiContainer(
    val original: ContainerLowLevel
) {
    val volumes by lazy {
        original.mounts.filter { it.type == HostConfig.Mount.Type.Volume }
            .map(::MountPoint)
    }

    val endpoints by lazy {
        original.networkSettings.networks.map {
            EndpointSettings(
                name = it.key,
                original = it.value
            )
        }
    }

    val id: String
        inline get() = original.id

    val isRunning: Boolean
        inline get() = original.state.status.isRunning

    val pid: Int
        inline get() = original.state.pid

    val state: Container.State
        inline get() = original.state.status

    val name by lazy {
        original.name.name()
    }

    val command by lazy {
        original.path + original.args.joinToString(separator = " ", prefix = " ")
    }

    val lastStarted by lazy {
        val value = Clock.System.now() - original.state.startedAt
        val resources = ContextCompat.getContext().resources
        when {
            value >= 1.days -> {
                val days = value.inWholeDays
                resources.getQuantityString(
                    R.plurals.container_last_started_day,
                    days.toInt(),
                    days
                )
            }

            value >= 1.hours -> {
                val hours = value.inWholeHours
                resources.getQuantityString(
                    R.plurals.container_last_started_hour,
                    hours.toInt(),
                    hours
                )
            }

            value >= 1.minutes -> {
                val minutes = value.inWholeMinutes
                resources.getQuantityString(
                    R.plurals.container_last_started_minute,
                    minutes.toInt(),
                    minutes
                )
            }

            else -> {
                val seconds = value.inWholeSeconds
                resources.getQuantityString(
                    R.plurals.container_last_started_second,
                    seconds.toInt(),
                    seconds
                )
            }
        }
    }

    val restartPolicy by lazy {
        with(original.hostConfig.restartPolicy) {
            if (name == HostConfig.RestartPolicy.Name.OnFailure) {
                "$name (${maximumRetryCount})"
            } else {
                "$name"
            }
        }
    }

    val ports by lazy {
        original.networkSettings.ports.mapNotNull {
            it.value?.first()?.hostPort?.let { port ->
                "${port}:${it.key}"
            }
        }
    }

    val image by lazy {
        original.config.image
    }

    val imageId by lazy {
        original.image.digest()
    }

    val imageSize by lazy {
        val sizeRw = original.sizeRw.sizeBySI()
        val sizeRootFs = original.sizeRootFs.sizeBySI()
        val imageSize = (original.sizeRootFs - original.sizeRw).sizeBySI()
        "$imageSize = $sizeRootFs - $sizeRw"
    }

    val createdAt by lazy {
        original.created.toLocalDateTime(TimeZone.currentSystemDefault())
            .copy(nanosecond = 0)
            .toString()
    }

    val composeProject by lazy {
        original.config.labels[Labels.COMPOSE_PROJECT]
    }

    val composeVersion by lazy {
        original.config.labels[Labels.COMPOSE_VERSION]
    }

    data class MountPoint(
        val original: ContainerLowLevel.MountPoint
    ) {
        val name: String
            inline get() = original.name

        val source: String
            inline get() = original.source

        val destination: String
            inline get() = original.destination
    }

    data class EndpointSettings(
        val name: String,
        val original: ContainerConfig.Networking.EndpointSettings
    ) {
        val id: String
            inline get() = original.networkId

        val macAddress: String
            inline get() = original.macAddress

        val dnsNames: List<String>
            inline get() = original.dnsNames

        val subnet by lazy {
            buildString {
                if (original.ipAddress.isNotEmpty()) {
                    append("${original.ipAddress}/${original.ipPrefixLen}")
                }
                if (original.globalIPv6Address.isNotEmpty()) {
                    appendLine()
                    append("${original.globalIPv6Address}/${original.globalIPv6PrefixLen}")
                }
            }
        }

        val gateway by lazy {
            buildString {
                if (original.gateway.isNotEmpty()) {
                    append(original.gateway)
                }
                if (original.ipv6Gateway.isNotEmpty()) {
                    appendLine()
                    append(original.ipv6Gateway)
                }
            }
        }
    }
}
