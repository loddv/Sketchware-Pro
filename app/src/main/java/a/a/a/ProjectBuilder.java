package a.a.a;

import static android.system.OsConstants.S_IRUSR;
import static android.system.OsConstants.S_IWUSR;
import static android.system.OsConstants.S_IXUSR;
import static org.jline.utils.Log.isDebugEnabled;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.system.Os;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;
import com.android.tools.r8.CompilationFailedException;
import com.github.megatronking.stringfog.plugin.StringFogClassInjector;
import com.github.megatronking.stringfog.plugin.StringFogMappingPrinter;
import com.iyxan23.zipalignjava.InvalidZipException;
import com.iyxan23.zipalignjava.ZipAlign;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import mod.agus.jcoderz.dex.Dex;
import mod.agus.jcoderz.dx.command.dexer.DxContext;
import mod.agus.jcoderz.dx.command.dexer.Main;
import mod.agus.jcoderz.dx.merge.CollisionPolicy;
import mod.agus.jcoderz.dx.merge.DexMerger;
import mod.agus.jcoderz.editor.library.ExtLibSelected;
import mod.agus.jcoderz.editor.manage.library.locallibrary.ManageLocalLibrary;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.compiler.kotlin.KotlinCompilerBridge;
import mod.hey.studios.project.ProjectSettings;
import mod.hey.studios.project.proguard.ProguardHandler;
import mod.hey.studios.util.SystemLogPrinter;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.build.compiler.dex.DexCompiler;
import mod.jbk.build.compiler.resource.ResourceCompiler;
import mod.jbk.util.LogUtil;
import mod.jbk.util.TestkeySignBridge;
import mod.pranav.build.JarBuilder;
import mod.pranav.build.R8Compiler;
import mod.pranav.viewbinding.ViewBindingBuilder;
import pro.sketchware.SketchApplication;
import pro.sketchware.util.library.BuiltInLibraryManager;
import pro.sketchware.utility.FilePathUtil;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.ParseException;
import proguard.ProGuard;

public class ProjectBuilder {
    public static final String TAG = "AppBuilder";
    private static final ThreadLocal<org.eclipse.jdt.internal.compiler.batch.Main> COMPILER_POOL =
            ThreadLocal.withInitial(() -> {
                return new org.eclipse.jdt.internal.compiler.batch.Main(
                        new PrintWriter(new NullOutputStream()),
                        new PrintWriter(new NullOutputStream()),
                        false,
                        null,
                        null);
            });
    private final File aapt2Binary;
    private final File aaptBinary;
    private final Context context;
    private final int parallelism = Runtime.getRuntime().availableProcessors();
    // Variáveis de instância (reutilizáveis por thread)
    private final ThreadLocal<CompilerResources> threadLocalResources = ThreadLocal.withInitial(CompilerResources::new);
    public BuildSettings build_settings;
    public yq yq;
    public FilePathUtil fpu;
    public ManageLocalLibrary mll;
    public BuiltInLibraryManager builtInLibraryManager;
    public String androidJarPath;
    public ProguardHandler proguard;
    public ProjectSettings settings;
    private Boolean usingAapt2 = false;
    private BuildProgressReceiver progressReceiver;
    private boolean buildAppBundle = false;
    private ArrayList<File> dexesToAddButNotMerge = new ArrayList<>();
    private long timestampResourceCompilationStarted;
    // ===============================
    // === MULTITHREADING SUPPORT ===
    private ExecutorService executor;

    public ProjectBuilder(Context context, yq yqVar) {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        SystemLogPrinter.start();

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    0);
            LogUtil.d(TAG,
                    "Running Sketchware Pro " + info.versionName + " (" + info.versionCode + ")");
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    0);
            long fileSizeInBytes = new File(applicationInfo.sourceDir).length();
            LogUtil.d(TAG,
                    "base.apk's size is " + Formatter.formatFileSize(context,
                            fileSizeInBytes) + " (" + fileSizeInBytes + " B)");
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e(TAG,
                    "Somehow failed to get package info about us!",
                    e);
        }

        usingAapt2 = new BuildSettings(yqVar.sc_id).getValue(
                BuildSettings.SETTING_RESOURCE_PROCESSOR,
                BuildSettings.SETTING_RESOURCE_PROCESSOR_AAPT
        ).equals(BuildSettings.SETTING_RESOURCE_PROCESSOR_AAPT2);

        aaptBinary = new File(context.getCacheDir(),
                "aapt");
        aapt2Binary = new File(context.getCacheDir(),
                "aapt2");
        build_settings = new BuildSettings(yqVar.sc_id);
        this.context = context;
        yq = yqVar;
        fpu = new FilePathUtil();
        mll = new ManageLocalLibrary(yqVar.sc_id);
        builtInLibraryManager = new BuiltInLibraryManager(yqVar.sc_id);
        File defaultAndroidJar = new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH,
                "android.jar");
        androidJarPath = build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH,
                defaultAndroidJar.getAbsolutePath());
        proguard = new ProguardHandler(yqVar.sc_id);
        settings = new ProjectSettings(yqVar.sc_id);
        initExecutor(); // Inicializa o pool de threads
    }

    public ProjectBuilder(BuildProgressReceiver buildAsyncTask, Context context, yq yqVar) {
        this(context,
                yqVar);
        progressReceiver = buildAsyncTask;
    }

    public static boolean hasFileChanged(String fileInAssets, String targetFile) {
        long length;
        File compareToFile = new File(targetFile);
        oB fileUtil = new oB();
        long lengthOfFileInAssets = fileUtil.a(SketchApplication.getContext(),
                fileInAssets);
        if (compareToFile.exists()) {
            length = compareToFile.length();
        } else {
            length = 0;
        }
        if (lengthOfFileInAssets == length) {
            return false;
        }

        fileUtil.a(compareToFile);
        fileUtil.a(SketchApplication.getContext(),
                fileInAssets,
                targetFile);
        return true;
    }

    private void initExecutor() {
        this.executor = Executors.newFixedThreadPool(parallelism);
        LogUtil.d(TAG,
                "Multithreading enabled with " + parallelism + " threads");
    }

    private void shutdownExecutor() {
        if (executor != null && ! executor.isShutdown()) {
            executor.shutdown();
            try {
                if (! executor.awaitTermination(60,
                        TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void compileResources() {
        try {
            timestampResourceCompilationStarted = System.currentTimeMillis();
            ResourceCompiler compiler = new ResourceCompiler(this,
                    usingAapt2 ? aapt2Binary : aaptBinary,
                    buildAppBundle,
                    progressReceiver,
                    usingAapt2);
            compiler.compile();
            LogUtil.d(TAG,
                    "Compiling resources took " + (System.currentTimeMillis() - timestampResourceCompilationStarted) + " ms");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void generateViewBinding() throws IOException, SAXException {
        if (settings.getValue(ProjectSettings.SETTING_ENABLE_VIEWBINDING,
                        ProjectSettings.SETTING_GENERIC_VALUE_FALSE)
                .equals(ProjectSettings.SETTING_GENERIC_VALUE_FALSE)) {
            return;
        }
        File outputDirectory = new File(yq.javaFilesPath + File.separator + yq.packageName.replace(".",
                File.separator) + File.separator + "databinding");
        outputDirectory.mkdirs();

        List<File> layouts = FileUtil.listFiles(yq.layoutFilesPath,
                        "xml").stream()
                .map(File::new)
                .collect(Collectors.toList());

        ViewBindingBuilder builder = new ViewBindingBuilder(layouts,
                outputDirectory,
                yq.packageName);
        builder.generateBindings();
    }

    public boolean isD8Enabled() {
        return build_settings.getValue(BuildSettings.SETTING_DEXER,
                        BuildSettings.SETTING_DEXER_DX)
                .equals(BuildSettings.SETTING_DEXER_D8);
    }

    public String getAaptRunningText() {
        return (usingAapt2 ? "Aapt" : "Aapt2") + " is running...";
    }

    public String getDxRunningText() {
        return (isD8Enabled() ? "D8" : "Dx") + " is running...";
    }

    public void createDexFilesFromClasses() throws Exception {
        FileUtil.makeDir(yq.binDirectoryPath + File.separator + "dex");
        if (proguard.isShrinkingEnabled() && proguard.isR8Enabled()) {return;}

        if (isD8Enabled()) {
            long savedTimeMillis = System.currentTimeMillis();
            try {
                // D8 já é multithreaded internamente
                CompletableFuture<Void> d8Future = CompletableFuture.runAsync(() -> {
                            try {
                                DexCompiler.compileDexFiles(this);
                            } catch (CompilationFailedException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        executor);

                d8Future.join(); // Espera terminar!
                LogUtil.d(TAG,
                        "D8 took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
            } catch (Exception e) {
                LogUtil.e(TAG,
                        "D8 failed",
                        e);
                throw e;
            }
        } else {
            long savedTimeMillis = System.currentTimeMillis();
            List<String> args = Arrays.asList(
                    "--debug",
                    "--verbose",
                    "--multi-dex",
                    "--output=" + yq.binDirectoryPath + File.separator + "dex",
                    proguard.isShrinkingEnabled() ? yq.proguardClassesPath : yq.compiledClassesPath
            );

            try {
                LogUtil.d(TAG,
                        "Running Dx with these arguments: " + args);
                Main.clearInternTables();
                Main.Arguments arguments = new Main.Arguments();
                Method parseMethod = Main.Arguments.class.getDeclaredMethod("parse",
                        String[].class);
                parseMethod.setAccessible(true);
                parseMethod.invoke(arguments,
                        (Object) args.toArray(new String[0]));
                Main.run(arguments);
                LogUtil.d(TAG,
                        "Dx took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
            } catch (Exception e) {
                LogUtil.e(TAG,
                        "Dx failed to process .class files",
                        e);
                throw e;
            }
        }
    }

    public String getClasspath() {
        StringBuilder classpath = new StringBuilder();
        KotlinCompilerBridge.maybeAddKotlinFilesToClasspath(classpath,
                yq);
        classpath.append(androidJarPath);

        if (! build_settings.getValue(BuildSettings.SETTING_NO_HTTP_LEGACY,
                        BuildSettings.SETTING_GENERIC_VALUE_FALSE)
                .equals(BuildSettings.SETTING_GENERIC_VALUE_TRUE)) {
            classpath.append(":").append(BuiltInLibraries.getLibraryClassesJarPathString(BuiltInLibraries.HTTP_LEGACY_ANDROID));
        }

        if (settings.getMinSdkVersion() < 21) {
            classpath.append(":").append(BuiltInLibraries.getLibraryClassesJarPathString(BuiltInLibraries.ANDROIDX_MULTIDEX));
        }

        if (! build_settings.getValue(BuildSettings.SETTING_JAVA_VERSION,
                        BuildSettings.SETTING_JAVA_VERSION_1_7)
                .equals(BuildSettings.SETTING_JAVA_VERSION_1_7)) {
            classpath.append(":").append(new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH,
                    "core-lambda-stubs.jar").getAbsolutePath());
        }

        for (Jp library : builtInLibraryManager.getLibraries()) {
            classpath.append(":").append(BuiltInLibraries.getLibraryClassesJarPathString(library.getName()));
        }

        classpath.append(mll.getJarLocalLibrary());

        if (! build_settings.getValue(BuildSettings.SETTING_CLASSPATH,
                "").isEmpty()) {
            classpath.append(":").append(build_settings.getValue(BuildSettings.SETTING_CLASSPATH,
                    ""));
        }

        String path = FileUtil.getExternalStorageDir() + "/.sketchware/data/" + yq.sc_id + "/files/classpath/";
        ArrayList<String> jars = FileUtil.listFiles(path,
                "jar");
        classpath.append(":").append(TextUtils.join(":",
                jars));

        return classpath.toString();
    }

    public String getProguardClasspath() {
        Collection<String> localLibraryJarsWithFullModeOn = new LinkedList<>();
        for (HashMap<String, Object> localLibrary : mll.list) {
            Object nameObject = localLibrary.get("name");
            Object jarPathObject = localLibrary.get("jarPath");
            if (nameObject instanceof String name && jarPathObject instanceof String jarPath) {
                if (localLibrary.containsKey("jarPath") && proguard.libIsProguardFMEnabled(name)) {
                    localLibraryJarsWithFullModeOn.add(jarPath);
                }
            }
        }

        String normalClasspath = getClasspath();
        StringBuilder classpath = new StringBuilder();
        normalClasspathLoop:
        for (String classpathPart : normalClasspath.split(":")) {
            for (String jarPathToExclude : localLibraryJarsWithFullModeOn) {
                if (classpathPart.equals(jarPathToExclude)) {
                    localLibraryJarsWithFullModeOn.remove(jarPathToExclude);
                    continue normalClasspathLoop;
                }
            }
            if (! classpathPart.equals(yq.compiledClassesPath)) {
                classpath.append(classpathPart).append(':');
            }
        }
        classpath.deleteCharAt(classpath.length() - 1);
        return classpath.toString();
    }

    private Collection<File> dexLibrariesParallel(File outputDirectory, List<File> dexes) throws Exception {
        if (dexes.isEmpty()) {return new ArrayList<>();}

        List<CompletableFuture<File>> chunkFutures = new ArrayList<>();
        int chunkSize = Math.max(1,
                dexes.size() / Math.max(1,
                        parallelism));

        for (int i = 0; i < dexes.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize,
                    dexes.size());
            List<File> chunk = new ArrayList<>(dexes.subList(start,
                    end));

            chunkFutures.add(CompletableFuture.supplyAsync(() -> {
                        try {
                            File chunkOutput = File.createTempFile("dex_chunk_",
                                    ".dex",
                                    outputDirectory);
                            mergeDexChunk(chunk,
                                    chunkOutput);
                            return chunkOutput;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor));
        }

        CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).join();
        List<File> chunkResults = chunkFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        return mergeFinalDexChunks(outputDirectory,
                chunkResults);
    }

    private void mergeDexChunk(List<File> dexFiles, File output) throws Exception {
        if (dexFiles.isEmpty()) {return;}
        List<Dex> dexes = dexFiles.stream()
                .map(file -> {
                    try {
                        return new Dex(new FileInputStream(file));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

        DexMerger merger = new DexMerger(dexes.toArray(new Dex[0]),
                CollisionPolicy.KEEP_FIRST,
                new DxContext());
        merger.merge().writeTo(output);
    }

    private Collection<File> mergeFinalDexChunks(File outputDirectory, List<File> chunks) throws Exception {
        Collection<File> result = new LinkedList<>();
        List<Dex> remaining = new ArrayList<>();

        for (File chunk : chunks) {
            remaining.add(new Dex(new FileInputStream(chunk)));
            if (remaining.size() >= 3) {
                File merged = new File(outputDirectory,
                        "classes" + (result.size() + 1) + ".dex");
                mergeDexes(merged,
                        remaining);
                result.add(merged);
                remaining.clear();
            }
        }

        if (! remaining.isEmpty()) {
            File finalMerged = new File(outputDirectory,
                    "classes" + (result.size() + 1) + ".dex");
            mergeDexes(finalMerged,
                    remaining);
            result.add(finalMerged);
        }

        for (File chunk : chunks)
            chunk.delete();
        return result;
    }

    public String getLibraryPackageNames() {
        StringBuilder extraPackages = new StringBuilder();
        for (Jp library : builtInLibraryManager.getLibraries()) {
            if (library.hasResources()) {
                extraPackages.append(library.getPackageName()).append(":");
            }
        }
        return extraPackages + mll.getPackageNameLocalLibrary();
    }

    public void compileJavaCode() throws zy, IOException {
        long savedTimeMillis = System.currentTimeMillis();
        CompilerResources res = threadLocalResources.get();
        res.reset();

        // Limpar argumentos antigos para evitar duplicações em chamadas repetidas
        ArrayList<String> args = res.args;
        args.clear();
        String javaVersion = System.getProperty("java.version");
        assert javaVersion != null;
        if (javaVersion.startsWith("1.") || javaVersion.compareTo("9") < 0) {
            // JDK 8 ou anterior
            args.add("-source");
            args.add(build_settings.getValue(BuildSettings.SETTING_JAVA_VERSION,
                    BuildSettings.SETTING_JAVA_VERSION_1_7)); // ou outra versão suportada
            args.add("-target");
            args.add(build_settings.getValue(BuildSettings.SETTING_JAVA_VERSION,
                    BuildSettings.SETTING_JAVA_VERSION_1_7));
        } else {
            // Adicionar versão Java atualizada para Java 25
            // JDK 9 ou superior
            args.add("--release");
            args.add("25"); // ou versão desejada moderna
        }

        args.add("-nowarn");

        if (! BuildSettings.SETTING_GENERIC_VALUE_TRUE.equals(
                build_settings.getValue(BuildSettings.SETTING_NO_WARNINGS,
                        BuildSettings.SETTING_GENERIC_VALUE_TRUE))) {
            args.add("-deprecation");
        }
        args.add("-d");
        args.add(yq.compiledClassesPath);
        args.add("-cp");
        args.add(getClasspath());
        args.add("-proc:none");
        args.add(yq.javaFilesPath);
        args.add(yq.rJavaDirectoryPath);

        // Adicionar arquivos de forma segura e centralizada
        addIfFileExists(args,
                fpu.getPathJava(yq.sc_id));
        addIfFileExists(args,
                fpu.getPathBroadcast(yq.sc_id));
        addIfFileExists(args,
                fpu.getPathService(yq.sc_id));

        // Deleção otimizada do arquivo R.java
        File rJavaFile = new File(yq.rJavaDirectoryPath,
                "R.java");
        if (rJavaFile.exists()) {
            try {
                if (! rJavaFile.delete()) {
                    LogUtil.w(TAG,
                            "Failed to delete R.java: " + rJavaFile.getAbsolutePath());
                }
            } catch (SecurityException e) {
                LogUtil.w(TAG,
                        "Permission denied deleting R.java",
                        e);
            }
        }

        try (PrintWriter outWriter = res.outWriter; PrintWriter errWriter = res.errWriter) {
            org.eclipse.jdt.internal.compiler.batch.Main main =
                    new org.eclipse.jdt.internal.compiler.batch.Main(outWriter,
                            errWriter,
                            false,
                            null,
                            null);

            if (isDebugEnabled()) {
                LogUtil.d(TAG,
                        "Compiling with args: " + args);
            }

            boolean success = main.compile(args.toArray(new String[0]));

            String stdout = res.outStream.getOut();
            String stderr = res.errStream.getOut();

            if (success && main.globalErrorsCount <= 0) {
                if (isDebugEnabled()) {
                    LogUtil.d(TAG,
                            "Compiler stdout: " + stdout);
                    LogUtil.d(TAG,
                            "Compiler stderr: " + stderr);
                    LogUtil.d(TAG,
                            "Compile time: " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
                }
            } else {
                LogUtil.e(TAG,
                        "Compile failed. Stderr: " + stderr);
                throw new zy(stderr.isEmpty() ? "Unknown compilation error" : stderr);
            }
        } finally {
            res.outWriter.flush();
            res.errWriter.flush();
        }
    }

    // Método auxiliar para adicionar argumento se arquivo existir
    private void addIfFileExists(ArrayList<String> args, String filePath) {
        if (FileUtil.isExistFile(filePath)) {
            args.add(filePath);
        }
    }

/*	public void compileJavaCode() throws zy, IOException {
		long savedTimeMillis = System.currentTimeMillis();
		CompilerResources res = threadLocalResources.get();
		res.reset();

		try (PrintWriter outWriter = res.outWriter; PrintWriter errWriter = res.errWriter) {

			// === Montagem eficiente de argumentos ===
			ArrayList<String> args = res.args;
			args.add("-" + build_settings.getValue(BuildSettings.SETTING_JAVA_VERSION, BuildSettings
			.SETTING_JAVA_VERSION_1_7));
			args.add("-nowarn");
			if (! BuildSettings.SETTING_GENERIC_VALUE_TRUE.equals(
					build_settings.getValue(BuildSettings.SETTING_NO_WARNINGS, BuildSettings
					.SETTING_GENERIC_VALUE_TRUE))) {
				args.add("-deprecation");
			}
			args.add("-d");
			args.add(yq.compiledClassesPath);
			args.add("-cp");
			args.add(getClasspath());
			args.add("-proc:none");
			args.add(yq.javaFilesPath);
			args.add(yq.rJavaDirectoryPath);

			String pathJava = fpu.getPathJava(yq.sc_id);
			if (FileUtil.isExistFile(pathJava)) args.add(pathJava);
			String pathBroadcast = fpu.getPathBroadcast(yq.sc_id);
			if (FileUtil.isExistFile(pathBroadcast)) args.add(pathBroadcast);
			String pathService = fpu.getPathService(yq.sc_id);
			if (FileUtil.isExistFile(pathService)) args.add(pathService);

			// === Deleção de R.java (otimizada) ===
			File rJavaFile = new File(yq.rJavaDirectoryPath, "R.java");
			if (rJavaFile.exists()) {
				try {
					if (! rJavaFile.delete()) {
						LogUtil.w(TAG, "Failed to delete R.java: " + rJavaFile.getAbsolutePath());
					}
				} catch (SecurityException e) {
					LogUtil.w(TAG, "Permission denied deleting R.java", e);
				}
			}

			// === Compilação ===
			org.eclipse.jdt.internal.compiler.batch.Main main =
					new org.eclipse.jdt.internal.compiler.batch.Main(outWriter, errWriter, false, null, null);

			if (isDebugEnabled()) {
				LogUtil.d(TAG, "Compiling with args: " + args);
			}

			boolean success = main.compile(args.toArray(new String[args.size()]));

			String stdout = res.outStream.getOut();
			String stderr = res.errStream.getOut();

			if (success && main.globalErrorsCount <= 0) {
				if (isDebugEnabled()) {
					LogUtil.d(TAG, "Compiler stdout: " + stdout);
					LogUtil.d(TAG, "Compiler stderr: " + stderr);
					LogUtil.d(TAG, "Compile time: " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
				}
			} else {
				LogUtil.e(TAG, "Compile failed. Stderr: " + stderr);
				throw new zy(stderr.isEmpty() ? "Unknown compilation error" : stderr);
			}

		} finally {
			// Garante limpeza mesmo em caso de exceção
			res.outWriter.flush();
			res.errWriter.flush();
		}
	}*/

    public void buildApk() throws By {
        String firstDexPath = dexesToAddButNotMerge.isEmpty() ? yq.classesDexPath :
                dexesToAddButNotMerge.remove(0).getAbsolutePath();
        try {
            ApkBuilder apkBuilder = new ApkBuilder(new File(yq.unsignedUnalignedApkPath),
                    new File(yq.resourcesApkPath),
                    new File(firstDexPath),
                    null,
                    null,
                    System.out);

            for (Jp library : builtInLibraryManager.getLibraries()) {
                apkBuilder.addResourcesFromJar(BuiltInLibraries.getLibraryClassesJarPath(library.getName()));
            }

            for (String jarPath : mll.getJarLocalLibrary().split(":")) {
                if (! jarPath.trim().isEmpty()) {
                    apkBuilder.addResourcesFromJar(new File(jarPath));
                }
            }

            File nativeLibrariesDirectory = new File(fpu.getPathNativelibs(yq.sc_id));
            if (nativeLibrariesDirectory.exists()) {
                apkBuilder.addNativeLibraries(nativeLibrariesDirectory);
            }

            for (String nativeLibraryDirectory : mll.getNativeLibs()) {
                apkBuilder.addNativeLibraries(new File(nativeLibraryDirectory));
            }

            if (dexesToAddButNotMerge.isEmpty()) {
                List<String> dexFiles = FileUtil.listFiles(yq.binDirectoryPath,
                        "dex");
                for (String dexFile : dexFiles) {
                    if (! Uri.fromFile(new File(dexFile)).getLastPathSegment().equals("classes.dex")) {
                        apkBuilder.addFile(new File(dexFile),
                                Uri.parse(dexFile).getLastPathSegment());
                    }
                }
            } else {
                int dexNumber = 2;
                for (File dexFile : dexesToAddButNotMerge) {
                    apkBuilder.addFile(dexFile,
                            "classes" + dexNumber + ".dex");
                    dexNumber++;
                }
            }

            apkBuilder.setDebugMode(false);
            apkBuilder.sealApk();
        } catch (ApkCreationException | SealedApkException e) {
            throw new By(e.getMessage());
        } catch (DuplicateFileException e) {
            String message = "Duplicate files from two libraries detected \r\nFile1: " + e.getFile1() + " \r\nFile2: "
                    + e.getFile2() + " \r\nArchive path: " + e.getArchivePath();
            throw new By(message);
        }
        LogUtil.d(TAG,
                "Time passed since starting to compile resources until building the unsigned APK: " +
                        (System.currentTimeMillis() - timestampResourceCompilationStarted) + " ms");
    }

    public void getDexFilesReadyParallel() throws Exception {
        long savedTimeMillis = System.currentTimeMillis();
        ArrayList<File> dexes = new ArrayList<>();

        if (settings.getMinSdkVersion() < 21) {
            dexes.add(BuiltInLibraries.getLibraryDexFile(BuiltInLibraries.ANDROIDX_MULTIDEX));
        }

        if (! build_settings.getValue(BuildSettings.SETTING_NO_HTTP_LEGACY,
                        ProjectSettings.SETTING_GENERIC_VALUE_FALSE)
                .equals(ProjectSettings.SETTING_GENERIC_VALUE_TRUE)) {
            dexes.add(BuiltInLibraries.getLibraryDexFile(BuiltInLibraries.HTTP_LEGACY_ANDROID));
        }

        for (Jp builtInLibrary : builtInLibraryManager.getLibraries()) {
            dexes.add(BuiltInLibraries.getLibraryDexFile(builtInLibrary.getName()));
        }

        ArrayList<HashMap<String, Object>> list = mll.list;
        for (int i1 = 0, listSize = list.size(); i1 < listSize; i1++) {
            HashMap<String, Object> localLibrary = list.get(i1);
            Object localLibraryName = localLibrary.get("name");
            if (localLibraryName instanceof String) {
                Object localLibraryDexPath = localLibrary.get("dexPath");
                if (localLibraryDexPath instanceof String) {
                    if (! proguard.libIsProguardFMEnabled((String) localLibraryName)) {
                        dexes.add(new File((String) localLibraryDexPath));
                        File localLibraryDirectory = new File((String) localLibraryDexPath).getParentFile();
                        if (localLibraryDirectory != null) {
                            File[] localLibraryFiles = localLibraryDirectory.listFiles();
                            if (localLibraryFiles != null) {
                                for (File localLibraryFile : localLibraryFiles) {
                                    String filename = localLibraryFile.getName();
                                    if (! filename.equals("classes.dex") && filename.startsWith("classes") && filename.endsWith(".dex")) {
                                        dexes.add(localLibraryFile);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    SketchwareUtil.toastError("Invalid DEX file path of enabled Local library #" + i1,
                            Toast.LENGTH_LONG);
                }
            } else {
                SketchwareUtil.toastError("Invalid name of enabled Local library #" + i1,
                        Toast.LENGTH_LONG);
            }
        }

        for (String file : FileUtil.listFiles(yq.binDirectoryPath + File.separator + "dex",
                "dex")) {
            dexes.add(new File(file));
        }

        LogUtil.d(TAG,
                "Will merge these " + dexes.size() + " DEX files to classes.dex: " + dexes);

        if (settings.getMinSdkVersion() < 21 || ! yq.N.isDebugBuild) {
            dexLibrariesParallel(new File(yq.binDirectoryPath),
                    dexes);
            LogUtil.d(TAG,
                    "Merging DEX files took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
        } else {
            dexesToAddButNotMerge = dexes;
            LogUtil.d(TAG,
                    "Skipped merging DEX files due to debug build with minSdkVersion >= 21");
        }
    }

    public void maybeExtractAapt2() throws By {
        var abi = Build.SUPPORTED_ABIS[0];
        /*String aaptPathInAssets = "aapt/aapt/";
        String aapt2PathInAssets = "aapt/aapt2/";
        if (abi.contains("64")) {
            if (abi.contains("x86")) {
                aaptPathInAssets += "aapt-x86_64";
                aapt2PathInAssets += "aapt2-x86_64";
            } else {
                aaptPathInAssets += "aapt-arm64";
                aapt2PathInAssets += "aapt2-arm64";
            }
        } else {
            if (abi.contains("x86")) {
                aaptPathInAssets += "aapt-x86";
                aapt2PathInAssets += "aapt2-x86";
            } else {
                aaptPathInAssets += "aapt-arm";
                aapt2PathInAssets += "aapt2-arm";
            }
        }*/
        /*if (hasFileChanged(aaptPathInAssets,
                    aaptBinary.getAbsolutePath())) {
                Os.chmod(aaptBinary.getAbsolutePath(),
                        S_IRUSR | S_IWUSR | S_IXUSR);
            }
            if (hasFileChanged(aapt2PathInAssets,
                    aapt2Binary.getAbsolutePath())) {
                Os.chmod(aapt2Binary.getAbsolutePath(),
                        S_IRUSR | S_IWUSR | S_IXUSR);
            }*/
        try {
            if (hasFileChanged("aapt/aapt2/aapt2-" + abi,
                    aapt2Binary.getAbsolutePath())) {
                Os.chmod(aapt2Binary.getAbsolutePath(),
                        S_IRUSR | S_IWUSR | S_IXUSR);
            }
            if (hasFileChanged("aapt/aapt/aapt-" + abi,
                    aaptBinary.getAbsolutePath())) {
                Os.chmod(aaptBinary.getAbsolutePath(),
                        S_IRUSR | S_IWUSR | S_IXUSR);
            }
        } catch (Exception e) {
            LogUtil.e(TAG,
                    "Failed to extract AAPT2 binaries",
                    e);
            throw new By("Couldn't extract AAPT2 binaries! Message: " + e.getMessage());
        }
    }

    public void buildBuiltInLibraryInformation() {
        if (yq.N.g) {
            builtInLibraryManager.addLibrary(BuiltInLibraries.ANDROIDX_APPCOMPAT);
            builtInLibraryManager.addLibrary(BuiltInLibraries.ANDROIDX_COORDINATORLAYOUT);
            builtInLibraryManager.addLibrary(BuiltInLibraries.MATERIAL);
        }
        if (yq.N.isFirebaseEnabled) {builtInLibraryManager.addLibrary(BuiltInLibraries.FIREBASE_COMMON);}
        if (yq.N.isFirebaseAuthUsed) {builtInLibraryManager.addLibrary(BuiltInLibraries.FIREBASE_AUTH);}
        if (yq.N.isFirebaseDatabaseUsed) {builtInLibraryManager.addLibrary(BuiltInLibraries.FIREBASE_DATABASE);}
        if (yq.N.isFirebaseStorageUsed) {builtInLibraryManager.addLibrary(BuiltInLibraries.FIREBASE_STORAGE);}
        if (yq.N.isMapUsed) {builtInLibraryManager.addLibrary(BuiltInLibraries.PLAY_SERVICES_MAPS);}
        if (yq.N.isAdMobEnabled) {builtInLibraryManager.addLibrary(BuiltInLibraries.PLAY_SERVICES_ADS);}
        if (yq.N.isGsonUsed) {builtInLibraryManager.addLibrary(BuiltInLibraries.GSON);}
        if (yq.N.isGlideUsed) {builtInLibraryManager.addLibrary(BuiltInLibraries.GLIDE);}
        if (yq.N.isHttp3Used) {builtInLibraryManager.addLibrary(BuiltInLibraries.OKHTTP_ANDROID);}

        KotlinCompilerBridge.maybeAddKotlinBuiltInLibraryDependenciesIfPossible(this,
                builtInLibraryManager);
        ExtLibSelected.addUsedDependencies(yq.N.x,
                builtInLibraryManager);
    }

    public BuiltInLibraryManager getBuiltInLibraryManager() {
        return builtInLibraryManager;
    }

    public void signDebugApk() throws GeneralSecurityException, IOException, ClassNotFoundException,
                                      IllegalAccessException, InstantiationException {
        TestkeySignBridge.signWithTestkey(yq.unsignedUnalignedApkPath,
                yq.finalToInstallApkPath);
    }

    private void mergeDexes(File target, List<Dex> dexes) throws IOException {
        DexMerger merger = new DexMerger(dexes.toArray(new Dex[0]),
                CollisionPolicy.KEEP_FIRST,
                new DxContext());
        Objects.requireNonNull(merger.merge()).writeTo(target);
    }

    private void proguardAddLibConfigs(List<String> args) {
        for (Jp library : builtInLibraryManager.getLibraries()) {
            File config = BuiltInLibraries.getLibraryProguardConfiguration(library.getName());
            if (config.exists()) {
                args.add("-include");
                args.add(config.getAbsolutePath());
            }
        }
    }

    private void proguardAddRjavaRules(List<String> args) {
        FileUtil.writeFile(yq.proguardAutoGeneratedExclusions,
                getRJavaRules());
        args.add("-include");
        args.add(yq.proguardAutoGeneratedExclusions);
    }

    private String getRJavaRules() {
        StringBuilder sb = new StringBuilder("# R.java rules\n");

        // Parte 1: builtInLibraryManager com paralelismo
        String builtInRules = builtInLibraryManager.getLibraries().parallelStream()
                .filter(jp -> jp.hasResources() && ! jp.getPackageName().isEmpty())
                .map(jp -> "-keep class " + jp.getPackageName() + ".** { *; }")
                .collect(Collectors.joining("\n"));

        // Parte 2: mll.list com paralelismo
        String mllRules = mll.list.parallelStream()
                .map(hashMap -> {
                    String obj = Objects.toString(hashMap.get("name"),
                            "");
                    String packageName = Objects.toString(hashMap.get("packageName"),
                            "");
                    if (! obj.isEmpty() && ! proguard.libIsProguardFMEnabled(obj)) {
                        return "-keep class " + packageName + ".** { *; }";
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        sb.append(builtInRules).append("\n")
                .append(mllRules).append("\n")
                .append("-keep class ").append(yq.packageName).append(".R { *; }\n");

        return sb.toString();
    }

    public void runR8Parallel() throws IOException {
        long savedTimeMillis = System.currentTimeMillis();

        var rules = new ArrayList<>(Arrays.asList(getRJavaRules().split("\n")));
        // Cria a lista config usando stream paralelo
        List<String> builtInRulePaths = builtInLibraryManager.getLibraries().parallelStream()
                .map(library -> BuiltInLibraries.getLibraryProguardConfiguration(library.getName()))
                .filter(File::exists)
                .map(File::getAbsolutePath)
                .toList();

        ArrayList<String> config = new ArrayList<>();
        config.add(ProguardHandler.ANDROID_PROGUARD_RULES_PATH);
        config.add(yq.proguardAaptRules);
        config.add(proguard.getCustomProguardRules());
        config.addAll(builtInRulePaths);
        config.addAll(mll.getPgRules());

        // Paraleliza a montagem de jarPaths com stream paralelo
        List<String> jarPaths = mll.list.parallelStream()
                .map(hashMap -> Objects.toString(hashMap.get("name"),
                        ""))
                .filter(name -> ! name.isEmpty() && proguard.libIsProguardFMEnabled(name))
                .map(name -> Objects.toString(mll.list.stream()
                                .filter(h -> name.equals(h.get("name")))
                                .findFirst()
                                .map(h -> h.get("jarPath"))
                                .orElse(null),
                        ""))
                .filter(jarPath -> jarPath != null && ! jarPath.isEmpty())
                .toList();

        CompletableFuture<File> projectJarFuture = CompletableFuture.supplyAsync(() -> {
                    JarBuilder.INSTANCE.generateJar(new File(yq.compiledClassesPath));
                    return new File(yq.compiledClassesPath + ".jar");
                },
                executor);

        List<File> inputJars = jarPaths.stream().map(File::new).collect(Collectors.toList());
        inputJars.add(projectJarFuture.join());

        try {
            new R8Compiler(
                    rules,
                    config.toArray(new String[0]),
                    getProguardClasspath().split(":"),
                    inputJars.stream().map(File::getAbsolutePath).toArray(String[]::new),
                    settings.getMinSdkVersion(),
                    yq
            ).compile();
        } catch (Exception e) {
            throw new IOException(e);
        }

        LogUtil.d(TAG,
                "R8 took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
    }

    public void runProguardParallel() throws IOException {
        long savedTimeMillis = System.currentTimeMillis();
        ArrayList<String> args = new ArrayList<>();
        args.add("-include");
        args.add(ProguardHandler.ANDROID_PROGUARD_RULES_PATH);
        args.add("-include");
        args.add(yq.proguardAaptRules);
        args.add("-include");
        args.add(proguard.getCustomProguardRules());
        proguardAddLibConfigs(args);
        proguardAddRjavaRules(args);
        for (String rule : mll.getPgRules()) {
            args.add("-include");
            args.add(rule);
        }
        args.add("-injars");
        args.add(yq.compiledClassesPath);
        for (HashMap<String, Object> hashMap : mll.list) {
            String obj = Objects.requireNonNull(hashMap.get("name")).toString();
            if (hashMap.containsKey("jarPath") && proguard.libIsProguardFMEnabled(obj)) {
                args.add("-injars");
                args.add(Objects.requireNonNull(hashMap.get("jarPath")).toString());
            }
        }
        args.add("-libraryjars");
        args.add(getProguardClasspath());
        args.add("-outjars");
        args.add(yq.proguardClassesPath);
        if (proguard.isDebugFilesEnabled()) {
            args.add("-printseeds");
            args.add(yq.proguardSeedsPath);
            args.add("-printusage");
            args.add(yq.proguardUsagePath);
            args.add("-printmapping");
            args.add(yq.proguardMappingPath);
        }
        LogUtil.d(TAG,
                "About to run ProGuard with these arguments: " + args);

        Configuration configuration = new Configuration();
        try {
            ConfigurationParser parser = new ConfigurationParser(args.toArray(new String[0]),
                    System.getProperties());
            try {
                parser.parse(configuration);
            } finally {
                parser.close();
            }
        } catch (ParseException e) {
            throw new IOException(e);
        }

        try {
            new ProGuard(configuration).execute();
        } catch (Exception e) {
            throw new IOException(e);
        }

        LogUtil.d(TAG,
                "ProGuard took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
    }

    public void runStringfog() {
        try {
            StringFogMappingPrinter printer = new StringFogMappingPrinter(new File(yq.binDirectoryPath,
                    "stringFogMapping.txt"));
            StringFogClassInjector injector = new StringFogClassInjector(new String[0],
                    "UTF-8",
                    "com.github.megatronking.stringfog.xor.StringFogImpl",
                    "com.github.megatronking.stringfog.xor.StringFogImpl",
                    printer);
            printer.startMappingOutput();
            printer.ouputInfo("UTF-8",
                    "com.github.megatronking.stringfog.xor.StringFogImpl");
            injector.doFog2ClassInDir(new File(yq.compiledClassesPath));
            KB.a(context,
                    "stringfog/stringfog.zip",
                    yq.compiledClassesPath);
        } catch (Exception e) {
            LogUtil.e("StringFog",
                    "Failed to run StringFog",
                    e);
        }
    }

    public void runZipalign(String inPath, String outPath) throws By {
        LogUtil.d(TAG,
                "About to zipalign " + inPath + " to " + outPath);
        long savedTimeMillis = System.currentTimeMillis();
        try (RandomAccessFile in = new RandomAccessFile(inPath,
                "r");
             FileOutputStream out = new FileOutputStream(outPath)) {
            ZipAlign.alignZip(in,
                    out);
        } catch (IOException e) {
            throw new By("Couldn't run zipalign on " + inPath + " with output path " + outPath + ": " + Log.getStackTraceString(e));
        } catch (InvalidZipException e) {
            throw new By("Failed to zipalign due to the given zip being invalid: " + Log.getStackTraceString(e));
        }
        LogUtil.d(TAG,
                "zipalign took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
    }

    public void setBuildAppBundle(boolean buildAppBundle) {
        this.buildAppBundle = buildAppBundle;
    }

    // === MÉTODO PRINCIPAL DE BUILD COM MULTITHREADING ===
    public void build() throws Exception {
        try {
            maybeExtractAapt2();
            buildBuiltInLibraryInformation();

            CompletableFuture<Void> resourcesFuture = CompletableFuture.runAsync(this::compileResources,
                    executor);
            CompletableFuture<Void> javaFuture = CompletableFuture.runAsync(() -> {
                        try {
                            compileJavaCode();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor);
            CompletableFuture<Void> bindingFuture = CompletableFuture.runAsync(() -> {
                        try {
                            generateViewBinding();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    },
                    executor);

            CompletableFuture.allOf(javaFuture,
                    bindingFuture).join();

            if (proguard.isShrinkingEnabled()) {
                if (proguard.isR8Enabled()) {runR8Parallel();} else {runProguardParallel();}
                runStringfog();
            }

            createDexFilesFromClasses();
            getDexFilesReadyParallel();
            resourcesFuture.join();

            buildApk();
            runZipalign(yq.unsignedUnalignedApkPath,
                    yq.finalToInstallApkPath);

            if (yq.N.isDebugBuild) {signDebugApk();}

            LogUtil.d(TAG,
                    "Total build time: " + (System.currentTimeMillis() - timestampResourceCompilationStarted) + " ms");
        } finally {
            shutdownExecutor();
        }

    }

    // Adicione isso na sua classe
    private static final class NullOutputStream extends OutputStream {
        static final NullOutputStream INSTANCE = new NullOutputStream();

        private NullOutputStream() {
        }

        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }

    private static class CompilerResources {
        final StringBuilder outBuffer = new StringBuilder(8192);
        final StringBuilder errBuffer = new StringBuilder(8192);
        final ArrayList<String> args = new ArrayList<>(30);
        final EclipseOutOutputStream outStream = new EclipseOutOutputStream();
        final EclipseErrOutputStream errStream = new EclipseErrOutputStream();
        final PrintWriter outWriter;
        final PrintWriter errWriter;

        CompilerResources() {
            outWriter = new PrintWriter(outStream,
                    false);
            errWriter = new PrintWriter(errStream,
                    false);
        }

        void reset() {
            outBuffer.setLength(0);
            errBuffer.setLength(0);
            args.clear();
            outStream.reset(outBuffer);
            errStream.reset(errBuffer);
            outWriter.flush();
            errWriter.flush();
        }
    }

    // Streams otimizados com StringBuilder reutilizável
    private static class EclipseOutOutputStream extends OutputStream {
        private StringBuilder buffer;

        void reset(StringBuilder sb) {
            this.buffer = sb;
        }

        @Override
        public void write(int b) {
            buffer.append((char) (b & 0xFF));
        }

        public String getOut() {
            return buffer.toString();
        }
    }

    private static class EclipseErrOutputStream extends OutputStream {
        private StringBuilder buffer;

        void reset(StringBuilder sb) {
            this.buffer = sb;
        }

        @Override
        public void write(int b) {
            buffer.append((char) (b & 0xFF));
        }

        public String getOut() {
            return buffer.toString();
        }
    }
}