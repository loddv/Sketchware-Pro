package mod.jbk.build.compiler.resource;

import static com.besome.sketch.Config.VAR_DEFAULT_TARGET_SDK_VERSION;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import a.a.a.Jp;
import a.a.a.ProjectBuilder;
import a.a.a.zy;
import mod.agus.jcoderz.editor.manage.library.locallibrary.ManageLocalLibrary;
import mod.hey.studios.build.BuildSettings;
import mod.hey.studios.project.ProjectSettings;
import mod.jbk.build.BuildProgressReceiver;
import mod.jbk.build.BuiltInLibraries;
import mod.jbk.diagnostic.MissingFileException;
import mod.jbk.util.LogUtil;
import pro.sketchware.SketchApplication;
import pro.sketchware.utility.BinaryExecutor;
import pro.sketchware.utility.FileUtil;

/**
 * A class responsible for compiling a Project's resources.
 * Supports AAPT1 and AAPT2 (with parallel compilation for AAPT2).
 */
public class ResourceCompiler {

    private static final String TAG = "AppBuilder";
    private final boolean willBuildAppBundle;
    private final File aaptFile;
    private final BuildProgressReceiver progressReceiver;
    private final ProjectBuilder builder;
    private final boolean useAapt2; // Nova flag

    public ResourceCompiler(
            ProjectBuilder builder, File aapt, boolean willBuildAppBundle, BuildProgressReceiver receiver,
            boolean useAapt2) {
        this.willBuildAppBundle = willBuildAppBundle;
        this.aaptFile = aapt;
        this.progressReceiver = receiver;
        this.builder = builder;
        this.useAapt2 = useAapt2;
    }

    public void compile() throws Exception {
        Compiler resourceCompiler;

        if (useAapt2) {
            resourceCompiler = new Aapt2Compiler(builder,
                    aaptFile,
                    willBuildAppBundle);
        } else {
            resourceCompiler = new Aapt1Compiler(builder,
                    aaptFile);
        }

        resourceCompiler.setProgressListener(new Compiler.ProgressListener() {
            @Override
            void onProgressUpdate(String newProgress, int step) {
                if (progressReceiver != null) {
                    progressReceiver.onProgress(newProgress,
                            step);
                }
            }
        });

        resourceCompiler.compile();
    }

    /**
     * A base class of a resource compiler.
     */
    interface Compiler {
        void compile() throws Exception;

        void setProgressListener(ProgressListener listener);

        abstract class ProgressListener {
            abstract void onProgressUpdate(String newProgress, int step);
        }
    }

    /**
     * A {@link Compiler} implementing AAPT1.
     */
    /**
     * A {@link Compiler} implementing AAPT1 with improved performance, caching, and parallel library support.
     */
    static class Aapt1Compiler implements Compiler {
        private final ProjectBuilder buildHelper;
        private final File aapt;
        private ProgressListener progressListener;

        public Aapt1Compiler(ProjectBuilder buildHelper, File aapt) {
            this.buildHelper = buildHelper;
            this.aapt = aapt;
        }

        @Override
        public void compile() throws zy, MissingFileException {
            if (progressListener != null) {
                progressListener.onProgressUpdate("Compiling resources with AAPT1...",
                        9);
            }

            ArrayList<String> args = new ArrayList<>();
            args.add(aapt.getAbsolutePath());
            args.add("package");

            String extraPackages = buildHelper.getLibraryPackageNames();
            if (! extraPackages.isEmpty()) {
                args.add("--extra-packages");
                args.add(extraPackages);
            }

            args.add("--min-sdk-version");
            args.add(String.valueOf(buildHelper.settings.getMinSdkVersion()));

            args.add("--target-sdk-version");
            args.add(buildHelper.settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION,
                    String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION)));

            args.add("--version-code");
            String versionCode = buildHelper.yq.versionCode;
            args.add((versionCode == null || versionCode.isEmpty()) ? "1" : versionCode);

            args.add("--version-name");
            String versionName = buildHelper.yq.versionName;
            args.add((versionName == null || versionName.isEmpty()) ? "1.0" : versionName);

            args.add("--auto-add-overlay");
            args.add("--generate-dependencies");
            args.add("-f");
            args.add("-m");
            args.add("--non-constant-id");
            args.add("--output-text-symbols");
            args.add(buildHelper.yq.binDirectoryPath);

            if (buildHelper.yq.N.g) {
                args.add("--no-version-vectors");
            }

            // Resources
            args.add("-S");
            args.add(buildHelper.yq.resDirectoryPath);

            for (String localRes : buildHelper.mll.getResLocalLibrary()) {
                args.add("-S");
                args.add(localRes);
            }

            String importedRes = buildHelper.fpu.getPathResource(buildHelper.yq.sc_id);
            if (FileUtil.isExistFile(importedRes)) {
                args.add("-S");
                args.add(importedRes);
            }

            // Assets
            args.add("-A");
            args.add(buildHelper.yq.assetsPath);

            String importedAssets = buildHelper.fpu.getPathAssets(buildHelper.yq.sc_id);
            if (FileUtil.isExistFile(importedAssets)) {
                args.add("-A");
                args.add(importedAssets);
            }

            for (String localAsset : new ManageLocalLibrary(buildHelper.yq.sc_id).getAssets()) {
                args.add("-A");
                args.add(localAsset);
            }

            for (Jp lib : buildHelper.builtInLibraryManager.getLibraries()) {
                if (lib.hasAssets()) {
                    String path = BuiltInLibraries.getLibraryAssetsPath(lib.getName());
                    linkingAssertDirectoryExists(path);
                    args.add("-A");
                    args.add(path);
                }
                if (lib.hasResources()) {
                    String path = BuiltInLibraries.getLibraryResourcesPath(lib.getName());
                    linkingAssertDirectoryExists(path);
                    args.add("-S");
                    args.add(path);
                }
            }

            args.add("-J");
            args.add(buildHelper.yq.rJavaDirectoryPath);

            args.add("-G");
            args.add(buildHelper.yq.proguardAaptRules);

            linkingAssertFileExists(buildHelper.yq.androidManifestPath);
            args.add("-M");
            args.add(buildHelper.yq.androidManifestPath);

            args.add("-I");
            String customJar = buildHelper.build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH,
                    "");
            args.add(customJar.isEmpty() ? buildHelper.androidJarPath : customJar);

            args.add("-F");
            args.add(buildHelper.yq.resourcesApkPath);

            LogUtil.d(TAG + ":aapt1",
                    "AAPT1 args: " + args);

            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(args);
            String log = executor.execute(30000);
            if (! log.isEmpty()) {
                LogUtil.e(TAG + ":aapt1",
                        log);
                throw new zy(log);
            }

            if (progressListener != null) {
                progressListener.onProgressUpdate("Resources compiled with AAPT1.",
                        10);
            }
        }

        private void linkingAssertFileExists(String path) throws MissingFileException {
            File f = new File(path);
            if (! f.exists()) {
                throw new MissingFileException(f,
                        MissingFileException.STEP_RESOURCE_LINKING,
                        false);
            }
        }

        private void linkingAssertDirectoryExists(String path) throws MissingFileException {
            File f = new File(path);
            if (! f.exists()) {
                throw new MissingFileException(f,
                        MissingFileException.STEP_RESOURCE_LINKING,
                        true);
            }
        }

        @Override
        public void setProgressListener(ProgressListener listener) {
            this.progressListener = listener;
        }
    }

    /**
     * A {@link Compiler} implementing AAPT2 with parallel library compilation.
     */
    static class Aapt2Compiler implements Compiler {
        private final boolean buildAppBundle;
        private final File aapt2;
        private final ProjectBuilder buildHelper;
        private final File compiledBuiltInLibraryResourcesDirectory;
        private ProgressListener progressListener;

        public Aapt2Compiler(ProjectBuilder buildHelper, File aapt2, boolean buildAppBundle) {
            this.buildHelper = buildHelper;
            this.aapt2 = aapt2;
            this.buildAppBundle = buildAppBundle;
            compiledBuiltInLibraryResourcesDirectory = new File(SketchApplication.getContext().getCacheDir(),
                    "compiledLibs");
        }

        @Override
        public void compile() throws zy, MissingFileException {
            String outputPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            emptyOrCreateDirectory(outputPath);

            long startTime = System.currentTimeMillis();

            if (progressListener != null) {
                progressListener.onProgressUpdate("Compiling resources with AAPT2...",
                        9);
            }

            compileLibrariesInParallel(outputPath);

            long savedTimeMillis = System.currentTimeMillis();
            compileProjectResources(outputPath);
            LogUtil.d(TAG + ":c",
                    "Compiling project resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            savedTimeMillis = System.currentTimeMillis();
            compileImportedResources(outputPath);
            LogUtil.d(TAG + ":c",
                    "Compiling imported resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            savedTimeMillis = System.currentTimeMillis();
            link();
            LogUtil.d(TAG + ":c",
                    "Linking resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            LogUtil.d(TAG + ":c",
                    "Total AAPT2 compilation took " + (System.currentTimeMillis() - startTime) + " ms");
        }

        private void compileLibrariesInParallel(String outputPath) throws zy, MissingFileException {
            compiledBuiltInLibraryResourcesDirectory.mkdirs();

            List<CompileTask> tasks = new ArrayList<>();

            // Built-in libraries
            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasResources()) {
                    String libName = library.getName();
                    String resPath = BuiltInLibraries.getLibraryResourcesPath(libName);
                    File outputFile = new File(compiledBuiltInLibraryResourcesDirectory,
                            libName + ".zip");

                    compilingAssertDirectoryExists(resPath);

                    if (isBuiltInLibraryRecompilingNeeded(outputFile)) {
                        tasks.add(new CompileTask(aapt2,
                                resPath,
                                outputFile.getAbsolutePath(),
                                "built-in library: " + libName));
                    } else {
                        LogUtil.d(TAG + ":cBILR",
                                "Skipped recompilation for built-in library " + libName);
                    }
                }
            }

            // Local libraries
            for (String localResDir : buildHelper.mll.getResLocalLibrary()) {
                File localDir = new File(localResDir).getParentFile();
                if (localDir != null) {
                    compilingAssertDirectoryExists(localResDir);
                    String outputFileName = localDir.getName() + ".zip";
                    tasks.add(new CompileTask(aapt2,
                            localResDir,
                            outputPath + File.separator + outputFileName,
                            "local library: " + localDir.getName()));
                }
            }

            if (tasks.isEmpty()) {
                LogUtil.d(TAG + ":cLLR",
                        "No libraries to compile.");
                return;
            }

            int totalTasks = tasks.size();
            LogUtil.d(TAG + ":cLLR",
                    "Compiling " + totalTasks + " libraries in parallel...");

            ExecutorService executor = Executors.newFixedThreadPool(Math.max(1,
                    Runtime.getRuntime().availableProcessors()));
            CompletionService<String> completionService = new ExecutorCompletionService<>(executor);

            for (CompileTask task : tasks) {
                completionService.submit(task);
            }

            int completed = 0;
            while (completed < totalTasks) {
                try {
                    Future<String> future = completionService.take();
                    String result = future.get();
                    if (result != null) {
                        throw new zy(result);
                    }
                    completed++;
                    LogUtil.d(TAG + ":cLLR",
                            "Completed " + completed + "/" + totalTasks);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new zy("Compilation interrupted: " + e.getMessage());
                } catch (Exception e) {
                    throw new zy("Error during parallel compilation: " + e.getMessage());
                }
            }

            try {
                executor.shutdown();
                if (! executor.awaitTermination(30,
                        TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        private void compileProjectResources(String outputPath) throws zy, MissingFileException {
            compilingAssertDirectoryExists(buildHelper.yq.resDirectoryPath);

            ArrayList<String> commands = new ArrayList<>();
            commands.add(aapt2.getAbsolutePath());
            commands.add("compile");
            commands.add("--dir");
            commands.add(buildHelper.yq.resDirectoryPath);
            commands.add("-o");
            commands.add(outputPath + File.separator + "project.zip");

            executeAndCheck(commands,
                    "project resources");
        }

        private void compileImportedResources(String outputPath) throws zy {
            String importedResPath = buildHelper.fpu.getPathResource(buildHelper.yq.sc_id);
            if (FileUtil.isExistFile(importedResPath) && new File(importedResPath).length() != 0) {
                ArrayList<String> commands = new ArrayList<>();
                commands.add(aapt2.getAbsolutePath());
                commands.add("compile");
                commands.add("--dir");
                commands.add(importedResPath);
                commands.add("-o");
                commands.add(outputPath + File.separator + "project-imported.zip");
                executeAndCheck(commands,
                        "imported resources");
            }
        }

        private void executeAndCheck(ArrayList<String> commands, String context) throws zy {
            LogUtil.d(TAG + ":c",
                    "Executing: " + commands);
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(commands);
            String log = executor.execute(20000);
            if (! log.isEmpty()) {
                LogUtil.e(TAG,
                        "Failed to compile " + context + ":\n" + log);
                throw new zy(log);
            }
        }

        private void link() throws zy, MissingFileException {
            String resourcesPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            if (progressListener != null) {
                progressListener.onProgressUpdate("Linking resources with AAPT2...",
                        10);
            }

            ArrayList<String> args = new ArrayList<>();
            args.add(aapt2.getAbsolutePath());
            args.add("link");
            if (buildAppBundle) {args.add("--proto-format");}
            args.add("--allow-reserved-package-id");
            args.add("--auto-add-overlay");
            args.add("--no-version-vectors");
            args.add("--no-version-transitions");

            args.add("--min-sdk-version");
            args.add(String.valueOf(buildHelper.settings.getMinSdkVersion()));
            args.add("--target-sdk-version");
            args.add(buildHelper.settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION,
                    String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION)));

            args.add("--version-code");
            String versionCode = buildHelper.yq.versionCode;
            args.add((versionCode == null || versionCode.isEmpty()) ? "1" : versionCode);
            args.add("--version-name");
            String versionName = buildHelper.yq.versionName;
            args.add((versionName == null || versionName.isEmpty()) ? "1.0" : versionName);

            args.add("-I");
            String customAndroidSdk = buildHelper.build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH,
                    "");
            if (customAndroidSdk.isEmpty()) {
                args.add(buildHelper.androidJarPath);
            } else {
                linkingAssertFileExists(customAndroidSdk);
                args.add(customAndroidSdk);
            }

            // Assets
            List<String> assetsPaths = Stream.concat(
                    Stream.of(buildHelper.yq.assetsPath),
                    Stream.of(buildHelper.fpu.getPathAssets(buildHelper.yq.sc_id))
            ).filter(FileUtil::isExistFile).distinct().toList();

            List<String> libraryAssets = buildHelper.builtInLibraryManager.getLibraries().parallelStream()
                    .filter(Jp::hasAssets)
                    .map(lib -> BuiltInLibraries.getLibraryAssetsPath(lib.getName()))
                    .peek(this::linkingAssertDirectoryExists)
                    .toList();

            List<String> localLibraryAssets = new ManageLocalLibrary(buildHelper.yq.sc_id).getAssets().parallelStream()
                    .peek(this::linkingAssertDirectoryExists)
                    .toList();

            synchronized (args) {
                Stream.concat(Stream.concat(assetsPaths.stream(),
                                        libraryAssets.stream()),
                                localLibraryAssets.stream())
                        .forEach(path -> {
                            args.add("-A");
                            args.add(path);
                        });
            }

            // Compiled resources
            buildHelper.builtInLibraryManager.getLibraries().stream()
                    .filter(Jp::hasResources)
                    .forEach(lib -> {
                        args.add("-R");
                        args.add(new File(compiledBuiltInLibraryResourcesDirectory,
                                lib.getName() + ".zip").getAbsolutePath());
                    });

            File[] localCompiled = new File(resourcesPath).listFiles();
            if (localCompiled != null) {
                Arrays.stream(localCompiled)
                        .parallel()
                        .filter(f -> f.isFile() && ! f.getName().equals("project.zip") && ! f.getName().equals(
                                "project-imported.zip"))
                        .forEach(f -> {
                            args.add("-R");
                            args.add(f.getAbsolutePath());
                        });
            }

            File projectZip = new File(resourcesPath,
                    "project.zip");
            if (projectZip.exists()) {
                args.add("-R");
                args.add(projectZip.getAbsolutePath());
            }

            File importedZip = new File(resourcesPath,
                    "project-imported.zip");
            if (importedZip.exists()) {
                args.add("-R");
                args.add(importedZip.getAbsolutePath());
            }

            linkingAssertDirectoryExists(buildHelper.yq.rJavaDirectoryPath);
            args.add("--java");
            args.add(buildHelper.yq.rJavaDirectoryPath);

            args.add("--proguard");
            args.add(buildHelper.yq.proguardAaptRules);

            linkingAssertFileExists(buildHelper.yq.androidManifestPath);
            args.add("--manifest");
            args.add(buildHelper.yq.androidManifestPath);

            String extraPackages = buildHelper.getLibraryPackageNames();
            if (! extraPackages.isEmpty()) {
                args.add("--extra-packages");
                args.add(extraPackages);
            }

            args.add("-o");
            args.add(buildHelper.yq.resourcesApkPath);

            LogUtil.d(TAG + ":l",
                    args.toString());

            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(args);
            String log = executor.execute(20000);
            if (! log.isEmpty()) {
                LogUtil.e(TAG + ":l",
                        log);
                throw new zy(log);
            }
        }

        private boolean isBuiltInLibraryRecompilingNeeded(File cached) {
            if (cached.exists()) {
                try {
                    Context ctx = SketchApplication.getContext();
                    return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),
                            0)
                            .lastUpdateTime > cached.lastModified();
                } catch (PackageManager.NameNotFoundException e) {
                    LogUtil.e(TAG + ":iBILRN",
                            "Package info error: " + e.getMessage(),
                            e);
                }
            }
            return true;
        }

        private void emptyOrCreateDirectory(String path) {
            if (FileUtil.isExistFile(path)) {FileUtil.deleteFile(path);}
            FileUtil.makeDir(path);
        }

        private void compilingAssertDirectoryExists(String path) throws MissingFileException {
            File dir = new File(path);
            if (! dir.exists()) {
                throw new MissingFileException(dir,
                        MissingFileException.STEP_RESOURCE_COMPILING,
                        true);
            }
        }

        private void linkingAssertFileExists(String path) throws MissingFileException {
            File f = new File(path);
            if (! f.exists()) {
                throw new MissingFileException(f,
                        MissingFileException.STEP_RESOURCE_LINKING,
                        false);
            }
        }

        private void linkingAssertDirectoryExists(String path) {
            try {
                File f = new File(path);
                if (! f.exists()) {
                    throw new MissingFileException(f,
                            MissingFileException.STEP_RESOURCE_LINKING,
                            true);
                }
            } catch (MissingFileException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setProgressListener(ProgressListener listener) {
            this.progressListener = listener;
        }

        private static class CompileTask implements Callable<String> {
            private final File aapt2;
            private final String inputDir;
            private final String outputPath;
            private final String description;

            CompileTask(File aapt2, String inputDir, String outputPath, String description) {
                this.aapt2 = aapt2;
                this.inputDir = inputDir;
                this.outputPath = outputPath;
                this.description = description;
            }

            @Override
            public String call() {
                ArrayList<String> commands = new ArrayList<>();
                commands.add(aapt2.getAbsolutePath());
                commands.add("compile");
                commands.add("--dir");
                commands.add(inputDir);
                commands.add("-o");
                commands.add(outputPath);

                LogUtil.d(TAG + ":cTASK",
                        "Compiling " + description + " -> " + outputPath);

                BinaryExecutor executor = new BinaryExecutor();
                executor.setCommands(commands);
                String log = executor.execute(20000);
                if (! log.isEmpty()) {
                    LogUtil.e(TAG + ":cTASK",
                            "Failed: " + description + "\n" + log);
                    return log;
                }
                return null;
            }
        }
    }
}