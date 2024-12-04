package it.fast4x.rimusic.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import it.fast4x.rimusic.R
import it.fast4x.rimusic.colorPalette
import it.fast4x.rimusic.enums.NavigationBarPosition
import it.fast4x.rimusic.enums.PopupType
import it.fast4x.rimusic.enums.ValidationType
import it.fast4x.rimusic.ui.components.themed.HeaderWithIcon
import it.fast4x.rimusic.ui.components.themed.SmartMessage
import it.fast4x.rimusic.ui.styling.Dimensions
import it.fast4x.rimusic.utils.*
import java.io.File
import java.net.Proxy

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current

//    var isAndroidAutoEnabled by remember {
//        val component = ComponentName(context, PlayerServiceModern::class.java)
//        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
//        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
//
//        mutableStateOf(
//            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
//            policy = object : SnapshotMutationPolicy<Boolean> {
//                override fun equivalent(a: Boolean, b: Boolean): Boolean {
//                    context.packageManager.setComponentEnabledSetting(
//                        component,
//                        if (b) enabledFlag else disabledFlag,
//                        PackageManager.DONT_KILL_APP
//                    )
//                    return a == b
//                }
//            }
//        )
//    }

    //var isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    var isProxyEnabled by rememberPreference(isProxyEnabledKey, false)
    var proxyHost by rememberPreference(proxyHostnameKey, "")
    var proxyPort by rememberPreference(proxyPortKey, 1080)
    var proxyMode by rememberPreference(proxyModeKey, Proxy.Type.HTTP)

    var defaultFolder by rememberPreference(defaultFolderKey, "/")

    var isKeepScreenOnEnabled by rememberPreference(isKeepScreenOnEnabledKey, false)

    //var checkUpdateState by rememberPreference(checkUpdateStateKey, CheckUpdateState.Disabled)

    var showFolders by rememberPreference(showFoldersOnDeviceKey, true)

    var blackListedPaths by remember {
        val file = File(context.filesDir, "Blacklisted_paths.txt")
        if (file.exists()) {
            mutableStateOf(file.readLines())
        } else {
            mutableStateOf(emptyList())
        }
    }

    var parentalControlEnabled by rememberPreference(parentalControlEnabledKey, false)
    var logDebugEnabled by rememberPreference(logDebugEnabledKey, false)

    var extraspace by rememberPreference(extraspaceKey, false)

    Column(
        modifier = Modifier
            .background(colorPalette().background0)
            //.fillMaxSize()
            .fillMaxHeight()
            .fillMaxWidth(
                if (NavigationBarPosition.Right.isCurrent())
                    Dimensions.contentWidthRightBar
                else
                    1f
            )
            .verticalScroll(rememberScrollState())
        /*
        .padding(
            LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
                .asPaddingValues()
        )

         */
    ) {
        HeaderWithIcon(
            title = stringResource(R.string.tab_miscellaneous),
            iconId = R.drawable.equalizer,
            enabled = false,
            showIcon = true,
            modifier = Modifier,
            onClick = {}
        )

    SettingsGroupSpacer()
    SettingsEntryGroupText(stringResource(R.string.on_device))
    StringListValueSelectorSettingsEntry(
        title = stringResource(R.string.blacklisted_folders),
        text = stringResource(R.string.edit_blacklist_for_on_device_songs),
        addTitle = stringResource(R.string.add_folder),
        addPlaceholder = if (isAtLeastAndroid10) {
            "Android/media/com.whatsapp/WhatsApp/Media"
        } else {
            "/storage/emulated/0/Android/media/com.whatsapp/"
        },
        conflictTitle = stringResource(R.string.this_folder_already_exists),
        removeTitle = stringResource(R.string.are_you_sure_you_want_to_remove_this_folder_from_the_blacklist),
        context = LocalContext.current,
        list = blackListedPaths,
        add = { newPath ->
            blackListedPaths = blackListedPaths + newPath
            val file = File(context.filesDir, "Blacklisted_paths.txt")
            file.writeText(blackListedPaths.joinToString("\n"))
        },
        remove = { path ->
            blackListedPaths = blackListedPaths.filter { it != path }
            val file = File(context.filesDir, "Blacklisted_paths.txt")
            file.writeText(blackListedPaths.joinToString("\n"))
        }
    )

    SwitchSettingEntry(
        title = stringResource(R.string.folders),
        text = stringResource(R.string.show_folders_in_on_device_page),
        isChecked = showFolders,
        onCheckedChange = { showFolders = it }
    )
    AnimatedVisibility(visible = showFolders) {
        TextDialogSettingEntry(
            title = stringResource(R.string.folder_that_will_show_when_you_open_on_device_page),
            text = defaultFolder,
            currentText = defaultFolder,
            onTextSave = { defaultFolder = it }
        )
    }

    SettingsGroupSpacer()

//    SettingsEntryGroupText(title = stringResource(R.string.android_auto))
//
//    SettingsDescription(text = stringResource(R.string.enable_unknown_sources))
//
//    SwitchSettingEntry(
//        title = stringResource(R.string.android_auto_1),
//        text = stringResource(R.string.enable_android_auto_support),
//        isChecked = isAndroidAutoEnabled,
//        onCheckedChange = { isAndroidAutoEnabled = it }
//    )
//
//    SettingsGroupSpacer()

    SettingsEntryGroupText(title = stringResource(R.string.androidheadunit))
    SwitchSettingEntry(
        title = stringResource(R.string.extra_space),
        text = "",
        isChecked = extraspace,
        onCheckedChange = { extraspace = it }
    )

    SettingsGroupSpacer()

    SettingsEntryGroupText(title = stringResource(R.string.service_lifetime))

    SwitchSettingEntry(
        title = stringResource(R.string.keep_screen_on),
        text = stringResource(R.string.prevents_screen_timeout),
        isChecked = isKeepScreenOnEnabled,
        onCheckedChange = { isKeepScreenOnEnabled = it }
    )

    ImportantSettingsDescription(text = stringResource(R.string.battery_optimizations_applied))

    if (isAtLeastAndroid12) {
        SettingsDescription(text = stringResource(R.string.is_android12))
    }

    val msgNoBatteryOptim = stringResource(R.string.not_find_battery_optimization_settings)

    SettingsEntry(
        title = stringResource(R.string.ignore_battery_optimizations),
        isEnabled = !isIgnoringBatteryOptimizations,
        text = if (isIgnoringBatteryOptimizations) {
            stringResource(R.string.already_unrestricted)
        } else {
            stringResource(R.string.disable_background_restrictions)
        },
        onClick = {
            if (!isAtLeastAndroid6) return@SettingsEntry

            try {
                activityResultLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            } catch (e: ActivityNotFoundException) {
                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                } catch (e: ActivityNotFoundException) {
                    SmartMessage(
                        "$msgNoBatteryOptim RiMusic",
                        type = PopupType.Info,
                        context = context
                    )
                }
            }
        }
    )

        /*
    SwitchSettingEntry(
        title = stringResource(R.string.invincible_service),
        text = stringResource(R.string.turning_off_battery_optimizations_is_not_enough),
        isChecked = isInvincibilityEnabled,
        onCheckedChange = { isInvincibilityEnabled = it }
    )

         */

    SettingsGroupSpacer()

    SettingsGroupSpacer()
    SettingsEntryGroupText(title = stringResource(R.string.proxy))
    SettingsDescription(text = stringResource(R.string.restarting_rimusic_is_required))
    SwitchSettingEntry(
        title = stringResource(R.string.enable_proxy),
        text = "",
        isChecked = isProxyEnabled,
        onCheckedChange = { isProxyEnabled = it }
    )

    AnimatedVisibility(visible = isProxyEnabled) {
        Column {
            EnumValueSelectorSettingsEntry(title = stringResource(R.string.proxy_mode),
                selectedValue = proxyMode,
                onValueSelected = { proxyMode = it },
                valueText = { it.name }
            )
            TextDialogSettingEntry(
                title = stringResource(R.string.proxy_host),
                text = proxyHost, //stringResource(R.string.set_proxy_hostname),
                currentText = proxyHost,
                onTextSave = { proxyHost = it },
                validationType = ValidationType.Ip
            )
            TextDialogSettingEntry(
                title = stringResource(R.string.proxy_port),
                text = proxyPort.toString(), //stringResource(R.string.set_proxy_port),
                currentText = proxyPort.toString(),
                onTextSave = { proxyPort = it.toIntOrNull() ?: 1080 })
        }
    }

    SettingsGroupSpacer()

    SettingsEntryGroupText(title = stringResource(R.string.parental_control))

    SwitchSettingEntry(
        title = stringResource(R.string.parental_control),
        text = stringResource(R.string.info_prevent_play_songs_with_age_limitation),
        isChecked = parentalControlEnabled,
        onCheckedChange = { parentalControlEnabled = it }
    )


    SettingsGroupSpacer()

    var text by remember { mutableStateOf(null as String?) }
    val noLogAvailable = stringResource(R.string.no_log_available)

    SettingsEntryGroupText(title = stringResource(R.string.debug))
    SwitchSettingEntry(
        title = stringResource(R.string.enable_log_debug),
        text = stringResource(R.string.if_enabled_create_a_log_file_to_highlight_errors),
        isChecked = logDebugEnabled,
        onCheckedChange = {
            logDebugEnabled = it
            if (!it) {
                val file = File(context.filesDir.resolve("logs"), "RiMusic_log.txt")
                if (file.exists())
                    file.delete()

                val filec = File(context.filesDir.resolve("logs"), "RiMusic_crash_log.txt")
                if (filec.exists())
                    filec.delete()


            } else
                SmartMessage(
                    context.resources.getString(R.string.restarting_rimusic_is_required),
                    type = PopupType.Info, context = context
                )
        }
    )
    ImportantSettingsDescription(text = stringResource(R.string.restarting_rimusic_is_required))
    ButtonBarSettingEntry(
        isEnabled = logDebugEnabled,
        title = stringResource(R.string.copy_log_to_clipboard),
        text = "",
        icon = R.drawable.copy,
        onClick = {
            val file = File(context.filesDir.resolve("logs"), "RiMusic_log.txt")
            if (file.exists()) {
                text = file.readText()
                text?.let {
                    textCopyToClipboard(it, context)
                }
            } else
                SmartMessage(noLogAvailable, type = PopupType.Info, context = context)
        }
    )
    ButtonBarSettingEntry(
        isEnabled = logDebugEnabled,
        title = stringResource(R.string.copy_crash_log_to_clipboard),
        text = "",
        icon = R.drawable.copy,
        onClick = {
            val file = File(context.filesDir.resolve("logs"), "RiMusic_crash_log.txt")
            if (file.exists()) {
                text = file.readText()
                text?.let {
                    textCopyToClipboard(it, context)
                }
            } else
                SmartMessage(noLogAvailable, type = PopupType.Info, context = context)
        }
    )

    Spacer(modifier = Modifier.height(Dimensions.bottomSpacer))

    }
}


