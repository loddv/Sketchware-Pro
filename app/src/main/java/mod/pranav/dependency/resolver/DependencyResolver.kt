package mod.pranav.dependency.resolver

import android.os.Environment
import android.util.Log
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
import org.w3c.dom.Node
import org.xml.sax.InputSource
import pro.sketchware.utility.FileUtil
import java.io.File
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.regex.Pattern
import java.util.zip.ZipFile
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DependencyResolver(
    private val groupId: String,
    private val artifactId: String,
    private var version: String,               // pode ser "latest" ou "new"
    private val skipDependencies: Boolean,
    private val buildSettings: BuildSettings
) {
    companion object {
        // Repositórios atualizados e funcionais em 2025 (removidos os obsoletos como JCenter)
        private val DEFAULT_REPOS = """
          |[
          |    {"url": "https://maven.google.com", "name": "Google Maven"},
          |    {"url": "https://repo.hortonworks.com/content/repositories/releases", "name": "HortanWorks"},
          |    {"url": "https://maven.atlassian.com/content/repositories/atlassian-public", "name": "Atlassian"},
          |    {"url": "https://jcenter.bintray.com", "name": "JCenter"},
          |    {"url": "https://oss.sonatype.org/content/repositories/releases", "name": "Sonatype Releases"},
          |    {"url": "https://repo.spring.io/plugins-release", "name": "Spring Plugins"},
          |    {"url": "https://repo.spring.io/release", "name": "Spring Release"},
          |    {"url": "https://repo.spring.io/libs-milestone", "name": "Spring Milestone"},
          |    {"url": "https://repo.maven.apache.org/maven2", "name": "Maven Central"}
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
        init()
    }

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
    }

    fun init() {
        val needsInitialization =
            Files.notExists(repositoriesJson) || Files.size(repositoriesJson) == 0L

        if (needsInitialization) {
            repositoriesJson.parent?.let { Files.createDirectories(it) }
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

    fun resolveDependency(callback: DependencyResolverCallback) = runBlocking {
        eventReciever = callback

        // === Resolução de versão "latest" ou "new" ===
        if (version.equals("latest", ignoreCase = true) || version.equals(
                "new",
                ignoreCase = true
            )
        ) {
            val tempArtifact = Artifact(groupId, artifactId, "latest", null, "")
            callback.onFetchingLatestVersion(tempArtifact)
            val resolved = resolveLatestVersion(groupId, artifactId)
            if (resolved == null) {
                callback.onVersionNotFound(tempArtifact)
                return@runBlocking
            }
            version = resolved
            callback.onFetchedLatestVersion(
                tempArtifact.apply { this.version = resolved },
                resolved
            )
        }

        val dependency = getArtifact(groupId, artifactId, version) ?: return@runBlocking

        if (dependency.extension != "jar" && dependency.extension != "aar") {
            callback.invalidPackaging(dependency)
            return@runBlocking
        }
        // === Preparação do classpath e library jars ===
        val libraryJars = listOf(
            BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.toPath()
                .resolve("core-lambda-stubs.jar"),
            Paths.get(
                buildSettings.getValue(
                    BuildSettings.SETTING_ANDROID_JAR_PATH,
                    BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH.resolve("android.jar").absolutePath
                )
            )
        )

        val dependencyClasspath = mutableListOf<Path>()
        val classpathSetting = buildSettings.getValue(BuildSettings.SETTING_CLASSPATH, "")
        classpathSetting.split(":").forEach {
            if (it.isNotEmpty()) dependencyClasspath.add(Paths.get(it))
        }

        // === Download da dependência principal ===
        val libDir = Paths.get(downloadPath, "${dependency.artifactId}-v${dependency.version}")
        val originalFile = libDir.resolve("classes.${dependency.extension}")
        Files.createDirectories(originalFile.parent)
        dependency.downloadTo(File(originalFile.toString()))

        // === Processamento de AAR (se for o caso) ===
        var inputJarForDex = originalFile
        if (dependency.extension == "aar") {
            callback.unzipping(dependency)
            unzip(originalFile)
            Files.delete(originalFile)

            val packageName = findPackageName(libDir.toString(), dependency.groupId)
            libDir.resolve("config").writeText(packageName)
            libDir.resolve("info")
                .writeText("${dependency.groupId}.${dependency.artifactId}:${dependency.version}")

            inputJarForDex = libDir.resolve("classes.jar")
        }

        // === Dexing da dependência principal ===
        callback.dexing(dependency)
        try {
            compileJar(
                outputJar = libDir.resolve("classes.jar"),
                inputJars = listOf(inputJarForDex),
                classpathJars = dependencyClasspath,
                libraryJars = libraryJars
            )
            callback.onResolutionComplete(dependency)
        } catch (e: Exception) {
            callback.dexingFailed(dependency, e)
            return@runBlocking
        }

        // === Se skipDependencies, finaliza aqui ===
        if (skipDependencies) {
            callback.onSkippingResolution(dependency)
            callback.onTaskCompleted(listOf("${dependency.artifactId}-v${dependency.version}"))
            return@runBlocking
        }

        // === Resolução de dependências transitivas ===
        dependency.resolveDependencyTree()
        val allDeps = dependency.getAllDependencies()

        // Baixa e processa todas as transitivas (sequencialmente, para evitar conflitos de classpath temporário)
        for (dep in allDeps) {
            if (dep.extension != "jar" && dep.extension != "aar") {
                callback.invalidPackaging(dep)
                continue
            }
            if (dep.version.isEmpty()) {
                callback.onVersionNotFound(dep)
                continue
            }

            val depDir = Paths.get(downloadPath, "${dep.artifactId}-v${dep.version}")
            val depFile = depDir.resolve("classes.${dep.extension}")
            Files.createDirectories(depFile.parent)
            dep.downloadTo(File(depFile.toString()))

            var depInputJar = depFile
            if (dep.extension == "aar") {
                callback.unzipping(dep)
                unzip(depFile)
                Files.delete(depFile)

                val packageName = findPackageName(depDir.toString(), dep.groupId)
                depDir.resolve("config").writeText(packageName)
                depDir.resolve("info").writeText("${dep.groupId}.${dep.artifactId}:${dep.version}")

                depInputJar = depDir.resolve("classes.jar")
            }

            // Adiciona ao classpath para as próximas dexings
            dependencyClasspath.add(depDir.resolve("classes.jar"))
        }

        // === Dexing paralelo das transitivas ===
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        runBlocking(executor.asCoroutineDispatcher()) {
            allDeps.forEach { dep ->
                launch {
                    val depDir = Paths.get(downloadPath, "${dep.artifactId}-v${dep.version}")
                    val jarToDex = depDir.resolve("classes.jar")

                    if (Files.notExists(jarToDex)) {
                        callback.onDependenciesNotFound(dep)
                        return@launch
                    }

                    callback.dexing(dep)
                    try {
                        compileJar(
                            outputJar = jarToDex,
                            inputJars = listOf(jarToDex), // já é o JAR extraído
                            classpathJars = dependencyClasspath.filter { it != jarToDex }, // remove próprio
                            libraryJars = libraryJars
                        )
                        callback.onResolutionComplete(dep)
                    } catch (e: Exception) {
                        callback.dexingFailed(dep, e)
                    }
                }
            }
        }

        callback.onTaskCompleted(allDeps.map { "${it.artifactId}-v${it.version}" })
    }

    // ====================== RESOLUÇÃO DE VERSÃO MAIS RECENTE ======================
    private fun resolveLatestVersion(groupId: String, artifactId: String): String? {
        val groupPath = groupId.replace('.', '/')
        for (repo in repositories) {
            val baseUrl = repo.getURL().replace("http://", "https://")
            val metadataUrl = "$baseUrl/$groupPath/$artifactId/maven-metadata.xml"
            try {
                val connection = URL(metadataUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "Cosmic-IDE/1.0")
                connection.setRequestProperty("Accept", "application/xml")
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = true

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.use { input ->
                        val factory = DocumentBuilderFactory.newInstance().apply {
                            isExpandEntityReferences = false
                            isCoalescing = true
                            try {
                                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                                setFeature(
                                    "http://apache.org/xml/features/disallow-doctype-decl",
                                    true
                                )
                                setFeature(
                                    "http://xml.org/sax/features/external-general-entities",
                                    false
                                )
                                setFeature(
                                    "http://xml.org/sax/features/external-parameter-entities",
                                    false
                                )
                                setFeature(
                                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                    false
                                )
                            } catch (ignored: Exception) {
                                Log.e(
                                    "DependencyResolver",
                                    "Falha ao configurar segurança XML",
                                    ignored
                                )
                            }
                        }

                        val builder = factory.newDocumentBuilder()
                        builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
                        val doc = builder.parse(input)
                        doc.documentElement.normalize()

                        fun getTextContent(tagName: String): String? {
                            // Estratégia 1: tag direta
                            doc.getElementsByTagName(tagName).item(0)?.textContent?.trim()
                                ?.takeIf { it.isNotEmpty() }?.let { return it }

                            // Estratégia 2: com namespace curinga
                            doc.getElementsByTagNameNS("*", tagName).item(0)?.textContent?.trim()
                                ?.takeIf { it.isNotEmpty() }?.let { return it }

                            // Estratégia 3: XPath
                            try {
                                val xpath = XPathFactory.newInstance().newXPath()
                                val node = xpath.compile("//$tagName")
                                    .evaluate(doc, XPathConstants.NODE) as? Node
                                node?.textContent?.trim()?.takeIf { it.isNotEmpty() }
                                    ?.let { return it }
                            } catch (ignored: Exception) {
                            }

                            // Estratégia 4: busca manual recursiva
                            fun findInNode(node: Node): String? {
                                if (node.nodeType == Node.ELEMENT_NODE && node.localName == tagName) {
                                    return node.textContent.trim().takeIf { it.isNotEmpty() }
                                }
                                val children = node.childNodes
                                for (i in 0 until children.length) {
                                    findInNode(children.item(i))?.let { return it }
                                }
                                return null
                            }
                            return findInNode(doc.documentElement)
                        }

                        return getTextContent("latest") ?: getTextContent("release")
                    }
                }
            } catch (ignored: Exception) {
                // Continua para o próximo repositório
            }
        }
        return null
    }

    // ====================== EXTRAÇÃO DE PACKAGE NAME ======================
    private fun findPackageName(path: String, defaultValue: String): String {
        val manifest = File(path).walkTopDown()
            .find { it.isFile && it.equals("AndroidManifest.xml") }
            ?: return defaultValue

        // Regex melhorado: suporta quebras de linha e espaços
        val pattern = Pattern.compile("""package\s*=\s*"(.*?)"""", Pattern.DOTALL)
        val matcher = pattern.matcher(manifest.readText())
        return if (matcher.find()) matcher.group(1)!! else defaultValue
    }

    // ====================== UNZIP ======================
    private fun unzip(path: Path) {
        ZipFile(path.toFile()).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val dest = path.parent.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    // ====================== COMPILAÇÃO COM D8 ======================
    private fun compileJar(
        outputJar: Path,           // classes.jar de saída (será sobrescrito)
        inputJars: List<Path>,     // JAR(s) com .class para dexar
        classpathJars: List<Path>, // Dependências já dexadas
        libraryJars: List<Path>    // android.jar + core-lambda-stubs.jar
    ) {
        Files.createDirectories(outputJar.parent)

        val builder = D8Command.builder()
            .setIntermediate(true)
            .setMode(CompilationMode.RELEASE)
            .setMinApiLevel(21)
            .setDisableDesugaring(false) // Habilita desugaring de Java 8+

            .addProgramFiles(inputJars)
            .addClasspathFiles(classpathJars)
            .addLibraryFiles(libraryJars)

            .setOutput(outputJar.parent, OutputMode.DexIndexed)

        D8.run(builder.build())
    }
}
