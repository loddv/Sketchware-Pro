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
    private static final int BUILD_PARALLELISM =
            Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));

    private static final ThreadLocal<org.eclipse.jdt.internal.compiler.batch.Main> COMPILER_POOL =
            ThreadLocal.withInitial(() -> new org.eclipse.jdt.internal.compiler.batch.Main(
                    new PrintWriter(new NullOutputStream()),
                    new PrintWriter(new NullOutputStream()),
                    true,
                    null,
                    null));

    private final File aapt2Binary;
    private final File aaptBinary;
    private final Context context;
    private final int parallelism = BUILD_PARALLELISM;

    private final ThreadLocal<CompilerResources> threadLocalResources = ThreadLocal.withInitial(CompilerResources::new);

    public BuildSettings build_settings;
    public yq yq;
    public FilePathUtil fpu;
    public ManageLocalLibrary mll;
    public BuiltInLibraryManager builtInLibraryManager;
    public String androidJarPath;
    public ProguardHandler proguard;
    public ProjectSettings settings;
    private boolean usingAapt2 = false;
    private BuildProgressReceiver progressReceiver;
    private boolean buildAppBundle = false;
    private ArrayList<File> dexesToAddButNotMerge = new ArrayList<>();
    private long timestampResourceCompilationStarted;

    private ExecutorService executor;

    public ProjectBuilder(Context context, yq yqVar) {
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        SystemLogPrinter.start();

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            LogUtil.d(TAG, "Running Sketchware Pro " + info.versionName + " (" + info.versionCode + ")");

            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);

            long fileSizeInBytes = new File(applicationInfo.sourceDir).length();
            LogUtil.d(TAG, "base.apk's size is " + Formatter.formatFileSize(context, fileSizeInBytes) + " (" + fileSizeInBytes + " B)");
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e(TAG, "Somehow failed to get package info about us!", e);
        }

        aaptBinary = new File(context.getCacheDir(), "aapt");
        aapt2Binary = new File(context.getCacheDir(), "aapt2");
        build_settings = new BuildSettings(yqVar.sc_id);
        usingAapt2 = isAAPT2Enabled();
        this.context = context;
        yq = yqVar;
        fpu = new FilePathUtil();
        mll = new ManageLocalLibrary(yqVar.sc_id);
        builtInLibraryManager = new BuiltInLibraryManager(yqVar.sc_id);
        File defaultAndroidJar = new File(BuiltInLibraries.EXTRACTED_COMPILE_ASSETS_PATH, "android.jar");
        androidJarPath = build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH, defaultAndroidJar.getAbsolutePath());
        proguard = new ProguardHandler(yqVar.sc_id);
        settings = new ProjectSettings(yqVar.sc_id);
        initExecutor();
    }

    public ProjectBuilder(BuildProgressReceiver buildAsyncTask, Context context, yq yqVar) {
        this(context, yqVar);
        progressReceiver = buildAsyncTask;
    }

    public static boolean hasFileChanged(String fileInAssets, String targetFile) {
        long length;
        File compareToFile = new File(targetFile);
        oB fileUtil = new oB();
        long lengthOfFileInAssets = fileUtil.a(SketchApplication.getContext(), fileInAssets);
        if (compareToFile.exists()) {
            length = compareToFile.length();
        } else {
            length = 0;
        }
        if (lengthOfFileInAssets == length) {
            return false;
        }
        fileUtil.a(compareToFile);
        fileUtil.a(SketchApplication.getContext(), fileInAssets, targetFile);
        return true;
    }

    private void initExecutor() {
        this.executor = Executors.newFixedThreadPool(parallelism);
        LogUtil.d(TAG, "Multithreading enabled with " + parallelism + " threads");
    }

    private void shutdownExecutor() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // ... mantenha o resto da classe igual ...

    public void createDexFilesFromClasses() throws Exception {
        FileUtil.makeDir(yq.binDirectoryPath + File.separator + "dex");
        if (proguard.isShrinkingEnabled() && proguard.isR8Enabled()) {
            return;
        }

        if (isD8Enabled()) {
            long savedTimeMillis = System.currentTimeMillis();
            try {
                CompletableFuture<Void> d8Future = CompletableFuture.runAsync(() -> {
                    try {
                        DexCompiler.compileDexFiles(this);
                    } catch (CompilationFailedException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);

                d8Future.join();
                LogUtil.d(TAG, "D8 took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
            } catch (Exception e) {
                LogUtil.e(TAG, "D8 failed", e);
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
                LogUtil.d(TAG, "Running Dx with these arguments: " + args);

                Main.clearInternTables();
                Main.Arguments arguments = new Main.Arguments();
                Method parseMethod = Main.Arguments.class.getDeclaredMethod("parse", String[].class);
                parseMethod.setAccessible(true);
                parseMethod.invoke(arguments, (Object) args.toArray(new String[0]));

                Main.run(arguments);
                LogUtil.d(TAG, "Dx took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");
            } catch (Exception e) {
                LogUtil.e(TAG, "Dx failed to process .class files", e);
                throw e;
            }
        }
    }

    public void compileJavaCode() throws zy, IOException {
        long savedTimeMillis = System.currentTimeMillis();
        CompilerResources res = threadLocalResources.get();
        res.reset();

        ArrayList<String> args = res.args;
        args.clear();

        int javaMajorVersion = getJavaMajorVersion();
        if (javaMajorVersion <= 8) {
            String targetVersion = build_settings.getValue(BuildSettings.SETTING_JAVA_VERSION, BuildSettings.SETTING_JAVA_VERSION_1_7);
            args.add("-source");
            args.add(targetVersion);
            args.add("-target");
            args.add(targetVersion);
        } else {
            String releaseVersion = build_settings.getValue(BuildSettings.SETTING_JAVA_VERSION, "17");
            args.add("--release");
            args.add(releaseVersion);
        }

        args.add("-nowarn");
        if (!BuildSettings.SETTING_GENERIC_VALUE_TRUE.equals(
                build_settings.getValue(BuildSettings.SETTING_NO_WARNINGS, BuildSettings.SETTING_GENERIC_VALUE_TRUE))) {
            args.add("-deprecation");
        }

        args.add("-d");
        args.add(yq.compiledClassesPath);
        args.add("-cp");
        args.add(getClasspath());

        // PATCH CONCRETO PARA ECJ PARALLEL
        args.add("-threads");
        args.add(String.valueOf(parallelism));

        args.add("-proc:none");
        args.add(yq.javaFilesPath);
        args.add(yq.rJavaDirectoryPath);

        addIfFileExists(args, fpu.getPathJava(yq.sc_id));
        addIfFileExists(args, fpu.getPathBroadcast(yq.sc_id));
        addIfFileExists(args, fpu.getPathService(yq.sc_id));

        File rJavaFile = new File(yq.rJavaDirectoryPath, "R.java");
        if (rJavaFile.exists()) {
            try {
                if (!rJavaFile.delete()) {
                    LogUtil.w(TAG, "Failed to delete R.java: " + rJavaFile.getAbsolutePath());
                }
            } catch (SecurityException e) {
                LogUtil.w(TAG, "Permission denied deleting R.java", e);
            }
        }

        try (PrintWriter outWriter = res.outWriter; PrintWriter errWriter = res.errWriter) {
            org.eclipse.jdt.internal.compiler.batch.Main main =
                    new org.eclipse.jdt.internal.compiler.batch.Main(outWriter, errWriter, false, null, null);

            if (isDebugEnabled()) {
                LogUtil.d(TAG, "Compiling with args: " + args);
            }

            boolean success = main.compile(args.toArray(new String[0]));
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
            res.outWriter.flush();
            res.errWriter.flush();
        }
    }
}
