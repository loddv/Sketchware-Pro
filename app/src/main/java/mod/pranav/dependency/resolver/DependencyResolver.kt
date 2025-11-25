package mod.pranav.dependency.resolver

import android.os.Environment
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mod.hey.studios.build.BuildSettings
import mod.hey.studios.util.Helper
import mod.jbk.build.BuiltInLibraries
import org.cosmic.ide.dependency.resolver.api.Artifact
import org.cosmic.ide.dependency.resolver.api.EventReciever
import org.cosmic.ide.dependency.resolver.api.Repository
import org.cosmic.ide.dependency.resolver.eventReciever
import org.cosmic.ide.dependency.resolver.getArtifact
import org.cosmic.ide.dependency.resolver.repositories
import pro.sketchware.utility.FileUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.zip.ZipFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DependencyResolver(
    private val groupId: String,
    private val artifactId: String,
    private var version: String, // Pode ser "latest", "release", "1.5+", "2.9.0" etc.
    private val skipDependencies: Boolean,
    private val buildSettings: BuildSettings
) {

    companion object {
        private val client = OkHttpClient()

        private val DEFAULT_REPOS = """
            |[
            |    {"url": "https://repo.maven.apache.org/maven2", "name": "Maven Central"},
            |    {"url": "https://maven.google.com", "name": "Google Maven"},
            |    {"url": "https://jcenter.bintray.com", "name": "JCenter"},
            |    {"url": "https://oss.sonatype.org/content/repositories/releases", "name": "Sonatype Releases"},
            |    {"url": "https://repo.spring.io/release", "name": "Spring Release"},
            |    {"url": "https://androidx.dev/storage/compose-compiler/repository", "name": "Compose Compiler Repo"}
            |]
        """.trimMargin()
    }

    private val downloadPath: String =
        FileUtil.getExternalStorageDir() + "/.sketchware/libs/local_libs"

    private val repositoriesJson = Paths.get(
        Environment.getExternalStorageDirectory().absolutePath,
        ".sketchware",
        "libs",
        "repositories.json"
    )

    private var resolvedVersion: String = version

    init {
        if (Files.notExists(repositoriesJson)) {
            Files.createDirectories(repositoriesJson.parent)
            repositoriesJson.writeText(DEFAULT_REPOS)
        }

        val reposList = Gson().fromJson(repositoriesJson.readText(), Helper.TYPE_MAP_LIST)
        reposList.forEach { map ->
            val url = (map["url"] as? String)?.removeSuffix("/") ?: return@forEach
            val name = map["name"] as String

            repositories.add(object : Repository {
                override fun getName(): String = name
                override fun getURL(): String = url
            })
        }
    }

    // ================================== CALLBACKS ==================================
    open class DependencyResolverCallback : EventReciever() {
        override fun artifactFound(artifact: Artifact) {}
        override fun onArtifactNotFound(artifact: Artifact) {}
        override fun onFetchingLatestVersion(artifact: Artifact) {}
        override fun onFetchedLatestVersion(artifact: Artifact, version: String) {}
        override fun onResolving(artifact: Artifact, dependency: Artifact) {}
        override fun onResolutionComplete(artifact: Artifact) {}
        override fun onSkippingResolution(artifact: Artifact) {}
        override fun onVersionNotFound(artifact: Artifact) {}
        override fun onDependenciesNotFound(artifact: Artifact) {}
        override fun onInvalidScope(artifact: Artifact, scope: String) {}
        override fun onInvalidPOM(artifact: Artifact) {}
        override fun onDownloadStart(artifact: Artifact) {}
        override fun onDownloadEnd(artifact: Artifact) {}
        override fun onDownloadError(artifact: Artifact, error: Throwable) {}
        open fun unzipping(artifact: Artifact) {}
        open fun dexing(artifact: Artifact) {}
        open fun onTaskCompleted(artifacts: List<String>) {}
        open fun dexingFailed(artifact: Artifact, e: Exception) {}
        open fun invalidPackaging(artifact: Artifact) {}
        open fun onNoVersionsFound(artifact: Artifact) {}
    }

    // ============================ RESOLUÇÃO DE VERSÃO DINÂMICA ============================
    private suspend fun resolveDynamicVersion(): String? = withContext(Dispatchers.IO) {
        val groupPath = groupId.replace(".", "/")
        val metadataPath = "$groupPath/$artifactId/maven-metadata.xml"

        for (repo in repositories) {
            val url = "${repo.getURL()}/$metadataPath"
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val xml = response.body?.string() ?: return@use null

                    // Prioridade: <release> e <latest> tags
                    val releaseTag = xml.substringAfter("<release>", "")
                        .substringBefore("</release>", "").trim()
                    if (releaseTag.isNotEmpty() && version.equals("release", ignoreCase = true)) {
                        return@withContext releaseTag
                    }

                    val latestTag = xml.substringAfter("<latest>", "")
                        .substringBefore("</latest>", "").trim()
                    if (latestTag.isNotEmpty() && version.equals("latest", ignoreCase = true)) {
                        return@withContext latestTag
                    }

                    // Extrai todas as versões disponíveis
                    val versionStrings = xml.split("<version>")
                        .drop(1)
                        .map { it.substringBefore("</version>").trim() }
                        .filter { it.isNotEmpty() }

                    if (versionStrings.isEmpty()) return@use null

                    val versions = versionStrings.map { ComparableVersion(it) }.sortedDescending()

                    return@withContext when {
                        version.equals("latest", ignoreCase = true) -> versions[0].toString()
                        version.equals("release", ignoreCase = true) -> {
                            versions.find { !it.isSnapshot }?.toString() ?: versions[0].toString()
                        }
                        version.endsWith("+") -> {
                            val base = version.dropLast(1).trim()
                            val baseVersion = ComparableVersion(if (base.isEmpty()) "0" else base)
                            versions.find { it >= baseVersion }?.toString()
                        }
                        else -> version // versão fixa
                    }
                }
            } catch (e: Exception) {
                // Tenta próximo repositório
            }
        }
        return@withContext null
    }

    // ================================== MÉTODO PRINCIPAL ==================================
    fun resolveDependency(callback: DependencyResolverCallback) = runBlocking {
        eventReciever = callback

        val isDynamicVersion = version.equals("latest", ignoreCase = true) ||
                version.equals("release", ignoreCase = true) ||
                version.endsWith("+")

        if (isDynamicVersion) {
            val tempArtifact = getArtifact(groupId, artifactId, version) ?: run {
                callback.onArtifactNotFound(Artifact(groupId, artifactId, version))
                return@runBlocking
            }

            callback.onFetchingLatestVersion(tempArtifact)

            val latest = resolveDynamicVersion()
            if (latest == null) {
                callback.onNoVersionsFound(tempArtifact)
                return@runBlocking
            }

            resolvedVersion = latest
            callback.onFetchedLatestVersion(tempArtifact.apply { this.version = latest }, latest)
        }

        val dependency = getArtifact(groupId, artifactId, resolvedVersion) ?: run {
            callback.onArtifactNotFound(Artifact(groupId, artifactId, resolvedVersion))
            return@runBlocking
        }

        if (dependency.extension != "jar" && dependency.extension != "aar") {
            callback.invalidPackaging(dependency)
            return@runBlocking
        }

        val libraryJars = listOf(
            BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.toPath().resolve("core-lambda-stubs.jar"),
            Paths.get(
                buildSettings.getValue(
                    BuildSettings.SETTING_ANDROID_JAR_PATH,
                    BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.resolve("android.jar").absolutePath
                )
            )
        )

        val dependencyClasspath = mutableListOf<Path>()
        buildSettings.getValue(BuildSettings.SETTING_CLASSPATH, "").split(":").forEach {
            if (it.isNotBlank()) dependencyClasspath.add(Paths.get(it))
        }

        // Download do artefato principal
        val outputFile = File("$downloadPath/${dependency.artifactId}-v${dependency.version}/classes.${dependency.extension}")
        outputFile.parentFile?.mkdirs()
        dependency.downloadTo(outputFile)

        // Processamento de AAR
        if (dependency.extension == "aar") {
            callback.unzipping(dependency)
            unzip(outputFile.toPath())
            Files.delete(outputFile.toPath())

            val packageName = findPackageName(outputFile.parentFile!!, dependency.groupId)
            Paths.get(outputFile.parent, "config").writeText(packageName)
        }

        val jarPath = Paths.get("$downloadPath/${dependency.artifactId}-v${dependency.version}/classes.jar")

        callback.dexing(dependency)
        try {
            compileJar(jarPath, dependencyClasspath, libraryJars)
            callback.onResolutionComplete(dependency)
        } catch (e: Exception) {
            callback.dexingFailed(dependency, e)
            return@runBlocking
        }

        if (skipDependencies) {
            callback.onSkippingResolution(dependency)
            callback.onTaskCompleted(listOf("${dependency.artifactId}-v${dependency.version}"))
            return@runBlocking
        }

        // Resolução de dependências transitivas
        dependency.resolveDependencyTree()

        for (dep in dependency.getAllDependencies()) {
            if (dep.version.isEmpty()) {
                callback.onVersionNotFound(dep)
                continue
            }

            if (dep.extension != "jar" && dep.extension != "aar") {
                callback.invalidPackaging(dep)
                continue
            }

            val depFile = File("$downloadPath/${dep.artifactId}-v${dep.version}/classes.${dep.extension}")
            depFile.parentFile?.mkdirs()
            dep.downloadTo(depFile)

            if (dep.extension == "aar") {
                callback.unzipping(dep)
                unzip(depFile.toPath())
                Files.delete(depFile.toPath())

                val pkg = findPackageName(depFile.parentFile!!, dep.groupId)
                Paths.get(depFile.parent, "config").writeText(pkg)
            }

            val depJar = Paths.get("$downloadPath/${dep.artifactId}-v${dep.version}/classes.jar")
            dependencyClasspath.add(depJar)

            callback.dexing(dep)
            try {
                compileJar(depJar, dependencyClasspath.toMutableList().apply { remove(depJar) }, libraryJars)
                callback.onResolutionComplete(dep)
            } catch (e: Exception) {
                callback.dexingFailed(dep, e)
            }
        }

        val resolvedList = dependency.getAllDependencies().map { "${it.artifactId}-v${it.version}" }
            .plus("${dependency.artifactId}-v${dependency.version}")

        callback.onTaskCompleted(resolvedList)
    }

    // ============================ LISTAR TODAS AS VERSÕES ============================
    suspend fun getAvailableVersions(): List<String> = withContext(Dispatchers.IO) {
        val groupPath = groupId.replace(".", "/")
        val metadataPath = "$groupPath/$artifactId/maven-metadata.xml"
        val allVersions = mutableSetOf<String>()

        for (repo in repositories) {
            val url = "${repo.getURL()}/$metadataPath"
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val xml = response.body?.string() ?: continue
                        val versions = xml.split("<version>")
                            .drop(1)
                            .map { it.substringBefore("</version>").trim() }
                            .filter { it.isNotEmpty() }
                        allVersions.addAll(versions)
                    }
                }
            } catch (e: Exception) {
                // Ignora erro e tenta próximo repo
            }
        }

        return@withContext allVersions
            .map { ComparableVersion(it) }
            .sortedDescending()
            .map { it.toString() }
    }

    // ================================== UTILITÁRIOS ==================================
    private fun findPackageName(path: File, defaultValue: String): String {
        val manifest = path.walkTopDown().find { it.isFile && it.name == "AndroidManifest.xml" }
            ?: return defaultValue
        val content = manifest.readText()
        val matcher = Pattern.compile("<manifest.*package=\"(.*?)\"").matcher(content)
        return if (matcher.find()) matcher.group(1)!! else defaultValue
    }

    private fun unzip(path: Path) {
        ZipFile(path.toFile()).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val destFile = path.parent.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(destFile)
                } else {
                    Files.createDirectories(destFile.parent)
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun compileJar(jarFile: Path, classpath: List<Path>, libraryJars: List<Path>) {
        Files.createDirectories(jarFile.parent)
        D8.run(
            D8Command.builder()
                .setIntermediate(true)
                .setMode(CompilationMode.RELEASE)
                .addProgramFiles(jarFile)
                .addLibraryFiles(libraryJars)
                .addClasspathFiles(classpath)
                .setOutput(jarFile.parent, OutputMode.DexIndexed)
                .build()
        )
    }

    // ============================= COMPARADOR DE VERSÕES =============================
    private class ComparableVersion(val version: String) : Comparable<ComparableVersion> {
        private val parts: List<Int>
        private val qualifier: String
        val isSnapshot: Boolean

        init {
            val cleanVersion = version.substringBefore("-")
            parts = cleanVersion.split('.').map { it.toIntOrNull() ?: 0 }
            qualifier = version.substringAfter("-", "")
            isSnapshot = version.contains("SNAPSHOT", ignoreCase = true)
        }

        override fun compareTo(other: ComparableVersion): Int {
            val maxLength = maxOf(parts.size, other.parts.size)
            for (i in 0 until maxLength) {
                val a = parts.getOrElse(i) { 0 }
                val b = other.parts.getOrElse(i) { 0 }
                if (a != b) return a.compareTo(b)
            }
            return when {
                qualifier.isEmpty() && other.qualifier.isNotEmpty() -> 1
                qualifier.isNotEmpty() && other.qualifier.isEmpty() -> -1
                else -> qualifier.compareTo(other.qualifier, ignoreCase = true)
            }
        }

        override fun toString(): String = version
    }
}
