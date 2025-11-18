package mod.pranav.dependency.resolver

import android.os.Environment
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.google.gson.Gson
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.regex.Pattern
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DependencyResolver(
    private val groupId: String,
    private val artifactId: String,
    private var version: String,               // agora pode ser "latest"
    private val skipDependencies: Boolean,
    private val buildSettings: BuildSettings
) {
    companion object {
        private val DEFAULT_REPOS = """
          |[
          |    {"url": "https://repo.hortonworks.com/content/repositories/releases", "name": "HortanWorks"},
          |    {"url": "https://maven.atlassian.com/content/repositories/atlassian-public", "name": "Atlassian"},
          |    {"url": "https://jcenter.bintray.com", "name": "JCenter"},
          |    {"url": "https://oss.sonatype.org/content/repositories/releases", "name": "Sonatype"},
          |    {"url": "https://repo.spring.io/plugins-release", "name": "Spring Plugins"},
          |    {"url": "https://repo.spring.io/libs-milestone", "name": "Spring Milestone"},
          |    {"url": "https://repo.maven.apache.org/maven2", "name": "Apache Maven"}
          |]
        """.trimMargin("|")
    }

    private val downloadPath: String =
        FileUtil.getExternalStorageDir() + "/.sketchware/libs/local_libs"

    private val repositoriesJson = Paths.get(
        Environment.getExternalStorageDirectory().absolutePath,
        ".sketchware",
        "libs",
        "repositories.json"
    )

    init {
        if (Files.notExists(repositoriesJson)) {
            Files.createDirectories(repositoriesJson.parent)
            repositoriesJson.writeText(DEFAULT_REPOS)
        }
        Gson().fromJson(repositoriesJson.readText(), Helper.TYPE_MAP_LIST).forEach {
            val url: String? = it["url"] as String?
            if (url != null) {
                repositories.add(object : Repository {
                    override fun getName(): String = it["name"] as String
                    override fun getURL(): String =
                        if (url.endsWith("/")) url.substringBeforeLast("/") else url
                })
            }
        }
    }

    open class DependencyResolverCallback : EventReciever() {
        // ... (mantidos todos os callbacks originais)
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
    }


    fun resolveDependency(callback: DependencyResolverCallback) = runBlocking {
        eventReciever = callback

        if (version.equals("latest", ignoreCase = true) || version.equals("new", ignoreCase = true)) {
            val tempArtifact = Artifact(groupId, artifactId, "latest", null, "")
            callback.onFetchingLatestVersion(tempArtifact)
            val resolved = resolveLatestVersion(groupId, artifactId)
            if (resolved == null) {
                callback.onVersionNotFound(tempArtifact)
                return@runBlocking
            }
            version = resolved
            callback.onFetchedLatestVersion(tempArtifact.apply { version = resolved }, resolved)
        }

        val dependency = getArtifact(groupId, artifactId, version) ?: return@runBlocking

        if (dependency.extension != "jar" && dependency.extension != "aar") {
            callback.invalidPackaging(dependency)
            return@runBlocking
        }

        val libraryJars = listOf(
            BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.toPath()
                .resolve("core-lambda-stubs.jar"), Paths.get(
                buildSettings.getValue(
                    BuildSettings.SETTING_ANDROID_JAR_PATH,
                    BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.resolve("android.jar").absolutePath
                )
            )
        )
        val dependencyClasspath = mutableListOf<Path>()

        val classpath = buildSettings.getValue(BuildSettings.SETTING_CLASSPATH, "")

        classpath.split(":").forEach {
            if (it.isEmpty()) return@forEach
            dependencyClasspath.add(Paths.get(it))
        }

        dependency.downloadTo(
            File(downloadPath + "/${dependency.artifactId}-v${dependency.version}/classes.${dependency.extension}")
                .apply {
                    parentFile?.mkdirs()
                }
        )

        if (dependency.extension == "aar") {
            callback.unzipping(dependency)
            unzip(
                Paths.get(
                    downloadPath,
                    "${dependency.artifactId}-v${dependency.version}",
                    "classes.aar"
                )
            )
            Files.delete(
                Paths.get(
                    downloadPath,
                    "${dependency.artifactId}-v${dependency.version}",
                    "classes.aar"
                )
            )
            val packageName = findPackageName(
                Paths.get(downloadPath, "${dependency.artifactId}-v${dependency.version}")
                    .toAbsolutePath().toString(),
                dependency.groupId
            )
            Paths.get(downloadPath, "${dependency.artifactId}-v${dependency.version}", "config")
                .writeText(packageName)
            Paths.get(downloadPath, "${dependency.artifactId}-v${dependency.version}", "info")
                .writeText(dependency.groupId + "." + dependency.artifactId + ":" + dependency.version)
        }

        val jar = Paths.get(
            downloadPath,
            "${dependency.artifactId}-v${dependency.version}",
            "classes.jar"
        )

        callback.dexing(dependency)
        try {
            compileJar(jar, dependencyClasspath, libraryJars)
            callback.onResolutionComplete(dependency)
        } catch (e: Exception) {
            callback.dexingFailed(dependency, e)
        }

        if (skipDependencies) {
            callback.onSkippingResolution(dependency)
            callback.onTaskCompleted(listOf("${dependency.artifactId}-v${dependency.version}"))
            return@runBlocking
        }
        dependency.resolveDependencyTree()

        dependency.getAllDependencies().forEach { dep ->
            println("Resolving dependency: ${dep.artifactId} v${dep.version}")
            if (dep.extension != "jar" && dep.extension != "aar") {
                callback.invalidPackaging(dep)
                return@forEach
            }

            if (dep.version.isEmpty()) {
                callback.onVersionNotFound(dep)
                return@forEach
            }

            val path = Paths.get(
                downloadPath,
                "${dep.artifactId}-v${dep.version}",
                "classes.${dep.extension}"
            )

            Files.createDirectories(path.parent)

            dep.downloadTo(File(path.toString()))

            if (dep.extension == "aar") {
                callback.unzipping(dep)
                unzip(path)
                Files.delete(path)
                val packageName =
                    findPackageName(path.parent.toAbsolutePath().toString(), dep.groupId)
                path.parent.resolve("config").writeText(packageName)
                path.parent.resolve("info")
                    .writeText(dep.groupId + "." + dep.artifactId + ":" + dep.version)
            }

            val jar = if (dep.extension == "jar") path else Paths.get(
                downloadPath, "${dep.artifactId}-v${dep.version}", "classes.jar"
            )
            if (Files.notExists(jar)) {
                callback.onDependenciesNotFound(dep)
                return@forEach
            }

            dependencyClasspath.add(jar)
        }

        // Use a fixed-size thread pool for dexing to avoid overwhelming the system
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        runBlocking(executor.asCoroutineDispatcher()) {
            dependency.getAllDependencies().forEach { dep ->
                launch {
                    val jar = Paths.get(
                        downloadPath,
                        "${dep.artifactId}-v${dep.version}",
                        "classes.jar"
                    )

                    callback.dexing(dep)
                    try {
                        compileJar(
                            jar,
                            dependencyClasspath.toMutableList().apply { remove(jar) },
                            libraryJars
                        )
                        callback.onResolutionComplete(dep)
                    } catch (e: Exception) {
                        callback.dexingFailed(dep, e)
                    }
                }
            }
        }

        callback.onTaskCompleted(
            dependency.getAllDependencies().map { "${it.artifactId}-v${it.version}" })
    }

    // ====================== NOVO METODO PARA latest ======================
    private fun resolveLatestVersion(groupId: String, artifactId: String): String? {
        val groupPath = groupId.replace('.', '/')
        for (repo in repositories) {
            val metadataUrl = "${repo.getURL()}/$groupPath/$artifactId/maven-metadata.xml"
            try {
                val connection = URL(metadataUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = true

                if (connection.responseCode == 200) {
                    val input = connection.inputStream
                    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(input)
                    doc.documentElement.normalize()

                    // Primeiro tenta <latest>, depois <release>
                    var latest = doc.getElementsByTagName("latest")
                        .takeIf { it.length > 0 }?.item(0)?.textContent?.trim()

                    if (latest.isNullOrEmpty()) {
                        latest = doc.getElementsByTagName("release")
                            .takeIf { it.length > 0 }?.item(0)?.textContent?.trim()
                    }

                    if (!latest.isNullOrEmpty()) {
                        return latest
                    }
                }
            } catch (ignored: Exception) {
                // continua tentando nos próximos repositórios
            }
        }
        return null
    }
    // =====================================================================

    private fun findPackageName(path: String, defaultValue: String): String {
        val manifest =
            File(path).walk().filter { it.isFile && it.name == "AndroidManifest.xml" }.firstOrNull()
        val content = manifest?.readText() ?: return defaultValue
        val p = Pattern.compile("<manifest.*package=\"(.*?)\"", Pattern.DOTALL)
        val m = p.matcher(content)
        if (m.find()) {
            return m.group(1)!!
        }

        return defaultValue
    }

    private fun unzip(path: Path) {
        val zipFile = ZipFile(path.toFile())
        zipFile.use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryDestination = path.parent.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(entryDestination)
                } else {
                    Files.createDirectories(entryDestination.parent)
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(entryDestination).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    private fun compileJar(jarFile: Path, jars: List<Path>, libraryJars: List<Path>) {
        Files.createDirectories(jarFile.parent)
        D8.run(
            D8Command.builder().setIntermediate(true).setMode(CompilationMode.RELEASE)
                .addProgramFiles(jarFile).addLibraryFiles(libraryJars).addClasspathFiles(jars)
                .setOutput(jarFile.parent, OutputMode.DexIndexed).build()
        )
    }
}