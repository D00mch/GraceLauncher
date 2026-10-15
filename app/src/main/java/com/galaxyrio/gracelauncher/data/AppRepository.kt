package com.galaxyrio.gracelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.galaxyrio.gracelauncher.SettingsActivity
import com.galaxyrio.gracelauncher.data.icons.IconPackRepository
import com.galaxyrio.gracelauncher.data.icons.IconDominantColor
import com.galaxyrio.gracelauncher.data.icons.IconSnapshotCache
import com.galaxyrio.gracelauncher.data.icons.ProfileIconSource
import com.galaxyrio.gracelauncher.data.icons.renderIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.LocalDate

data class WorkProfile(val user: UserHandle, val serial: Long)

class AppRepository(private val context: Context, private val iconPacks: IconPackRepository = IconPackRepository(context)) {
    private val packageManager = context.packageManager
    private data class ProfileSourceSignature(val configuration: Configuration, val day: LocalDate,
        val label: String, val installedAt: Long, val apkPaths: List<String>, val flags: Int, val revision: Long)
    private val profileSources = IconSnapshotCache<ProfileSourceSignature, LauncherApp>()
    private data class ProfilePackSignature(val source: Any?,
        val packs: List<com.galaxyrio.gracelauncher.data.icons.LoadedIconPack>, val day: LocalDate)
    private val profilePacks = IconSnapshotCache<ProfilePackSignature, LauncherApp>()
    private val profileRevisions = mutableMapOf<Pair<UserHandle, String>, Long>()

    /** Package changes invalidate only that installation, not every icon on unlock. */
    fun invalidateProfileApps(user: UserHandle, packages: Collection<String>) = synchronized(profileRevisions) {
        packages.forEach { name ->
            val key = user to name
            profileRevisions[key] = (profileRevisions[key] ?: 0L) + 1
        }
    }

    suspend fun loadApps(iconPackPackage: String?): List<LauncherApp> = loadApps(listOfNotNull(iconPackPackage))

    suspend fun loadApps(iconPackPackages: List<String> = emptyList()): List<LauncherApp> = withContext(Dispatchers.IO) {
        val packs = normalizeIconPackOrder(iconPackPackages).mapNotNull { iconPacks.load(it) }
        val coroutine = currentCoroutineContext()
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val englishLabels = EnglishLabelReader(context)
        val installTimes = mutableMapOf<String, Long>()
        resolved.asSequence()
            .mapNotNull { resolveInfo ->
                coroutine.ensureActive()
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                // Expose only our settings entry, never the launcher/HOME activity.
                if (activityInfo.packageName == context.packageName &&
                    activityInfo.name != SettingsActivity::class.java.name) return@mapNotNull null
                val component = ComponentName(activityInfo.packageName, activityInfo.name)
                val label = resolveInfo.loadLabel(packageManager)
                    .toString()
                    .trim()
                    .ifBlank { activityInfo.name.substringAfterLast('.') }
                val drawable = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull()
                val installedAt = installTimes.getOrPut(activityInfo.packageName) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= 33) {
                            packageManager.getPackageInfo(activityInfo.packageName, PackageManager.PackageInfoFlags.of(0L)).firstInstallTime
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(activityInfo.packageName, 0).firstInstallTime
                        }
                    }.getOrDefault(0L)
                }
                makeApp(component, label, drawable, packs, activityInfo.applicationInfo.flags)
                    .copy(firstInstallTime = installedAt, isLauncherSettings = activityInfo.packageName == context.packageName, englishLabel = englishLabels.label(resolveInfo))
            }
            .distinctBy(LauncherApp::key)
            .sortedWith(LauncherAppOrder)
            .toList()
    }

    fun workProfiles(): List<WorkProfile> {
        val launcher = context.getSystemService(LauncherApps::class.java)
        val users = context.getSystemService(UserManager::class.java)
        return launcher.profiles.filter { user ->
            user != Process.myUserHandle() && (Build.VERSION.SDK_INT < 35 ||
                launcher.getLauncherUserInfo(user)?.userType == UserManager.USER_TYPE_PROFILE_MANAGED)
        }.mapNotNull { user -> users.getSerialNumberForUser(user).takeIf { it >= 0 }?.let { WorkProfile(user, it) } }
    }

    suspend fun loadWorkApps(profiles: List<WorkProfile>, iconPackPackages: List<String>): List<LauncherApp> =
        profiles.flatMap { profile ->
            // Quiet/removed profiles must not prevent the personal app inventory from loading.
            try { loadProfileApps(profile.user, profile.serial, iconPackPackages, work = true) }
            catch (error: SecurityException) { emptyList() }
            catch (error: IllegalStateException) { emptyList() }
        }

    suspend fun loadPrivateApps(user: UserHandle, serial: Long, iconPackPackages: List<String>): List<LauncherApp> =
        loadProfileApps(user, serial, iconPackPackages, work = false)

    private suspend fun loadProfileApps(user: UserHandle, serial: Long, iconPackPackages: List<String>, work: Boolean): List<LauncherApp> = withContext(Dispatchers.IO) {
        val coroutine = currentCoroutineContext()
        val configuration = Configuration(context.resources.configuration)
        val day = LocalDate.now()
        val englishLabels = EnglishLabelReader(context)
        val apps = context.getSystemService(LauncherApps::class.java).getActivityList(null, user).map { activity ->
            coroutine.ensureActive()
            val component = activity.componentName
            val label = activity.label.toString().trim().ifBlank { component.shortClassName }
            val info = activity.applicationInfo
            val revision = synchronized(profileRevisions) { profileRevisions[user to component.packageName] ?: 0L }
            val signature = ProfileSourceSignature(configuration, day, label, activity.firstInstallTime,
                listOfNotNull(info.sourceDir) + info.splitSourceDirs.orEmpty(), info.flags, revision)
            val key = "profile:$serial:${component.flattenToString()}"
            profileSources.get(key, signature) ?: run {
                val drawable = runCatching { activity.getIcon(0) }.getOrNull()
                val source = runCatching { drawable?.let(ProfileIconSource::fromDrawable) }.getOrNull()
                makeApp(component, label, drawable, emptyList(), info.flags)
                    .let { app -> app.copy(user = user, userSerial = serial, isPrivateSpace = !work, isWorkProfile = work,
                        firstInstallTime = activity.firstInstallTime, profileIconSource = source, englishLabel = englishLabels.label(activity),
                        // Share the bounded source bitmap instead of retaining a second raw copy.
                        icon = source?.layers()?.original?.asImageBitmap() ?: app.icon) }
                    .also { app ->
                        coroutine.ensureActive()
                        // A transient profile permission/availability failure must be retried.
                        if (app.icon != null) profileSources.put(key, signature, app)
                    }
            }
        }.distinctBy(LauncherApp::key).sortedWith(LauncherAppOrder)
        applyPrivateIconPacks(apps, iconPackPackages)
    }

    suspend fun applyPrivateIconPacks(apps: List<LauncherApp>, iconPackPackages: List<String>): List<LauncherApp> = withContext(Dispatchers.IO) {
        val packs = normalizeIconPackOrder(iconPackPackages).mapNotNull { iconPacks.load(it) }
        if (packs.isEmpty()) return@withContext apps
        apps.map { app ->
            val signature = ProfilePackSignature(app.profileIconSource ?: app.icon, packs, LocalDate.now())
            val packed = profilePacks.get(app.key, signature) ?: run {
                val match = firstMatchingPackIcon(packs) { it.iconFor(app.componentName, null) }
                (if (match == null) app else app.copy(icon = match.second.bitmap, monochromeIcon = match.second.monochrome,
                    monochromeScale = match.second.monochromeScale, iconPackPackage = match.first.packageName,
                    themeIconPackPackage = match.first.packageName)).also { profilePacks.put(app.key, signature, it) }
            }
            app.copy(icon = packed.icon, monochromeIcon = packed.monochromeIcon, monochromeScale = packed.monochromeScale,
                iconPackPackage = packed.iconPackPackage, themeIconPackPackage = packed.themeIconPackPackage)
        }
    }

    private fun makeApp(component: ComponentName, label: String, drawable: Drawable?,
        packs: List<com.galaxyrio.gracelauncher.data.icons.LoadedIconPack>, flags: Int): LauncherApp {
        // Sample the provider's original drawable on IO, before considering packs.
        // Only the color survives; previews/drags never reload or resample icons.
        val originalColor = runCatching { drawable?.let {
            val sample = renderIcon(it, 48)
            try { IconDominantColor.of(sample) } finally { sample.recycle() }
        } }.getOrNull()
        // Generic masks/backgrounds do not intercept the remaining packs or the system fallback.
        val match = firstMatchingPackIcon(packs) { it.iconFor(component, null) }
        val packed = match?.second
        val icon = packed?.bitmap ?: runCatching { drawable?.toBitmap(144, 144)?.asImageBitmap() }.getOrNull()
        val monochrome = if (packed != null) packed.monochrome else if (Build.VERSION.SDK_INT >= 33) {
            runCatching {
                (drawable as? AdaptiveIconDrawable)?.monochrome?.toBitmap(144, 144)?.let { bitmap ->
                    val pixels = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    bitmap.takeIf { pixels.count { pixel -> pixel ushr 24 > 32 } >= pixels.size / 100 }?.asImageBitmap()
                }
            }.getOrNull()
        } else null
        return LauncherApp(componentName = component, label = label, icon = icon, monochromeIcon = monochrome,
            iconPackPackage = match?.first?.packageName, monochromeScale = packed?.monochromeScale ?: 1.4f,
            isSystemApp = flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
            isAdaptiveIcon = drawable is AdaptiveIconDrawable, originalIconColor = originalColor)
    }

    fun launch(
        app: LauncherApp,
        sourceBounds: Rect? = null,
        options: Bundle? = null,
    ): Boolean = runCatching {
        // This launcher API preserves the target's task semantics and lets Android
        // animate the launch from the real clicked icon, without private APIs.
        context.getSystemService(LauncherApps::class.java).startMainActivity(
            app.componentName,
            app.user ?: Process.myUserHandle(),
            sourceBounds,
            options,
        )
    }.isSuccess
}
