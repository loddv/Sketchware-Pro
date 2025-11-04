package mod.jbk.build.compiler.resource;

import static com.besome.sketch.Config.VAR_DEFAULT_TARGET_SDK_VERSION;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * Supports AAPT2 with parallel compilation of libraries.
 */
public class ResourceCompiler {

    private static final String TAG = "AppBuilder";
    private final boolean willBuildAppBundle;
    private final File aaptFile;
    private final BuildProgressReceiver progressReceiver;
    private final ProjectBuilder builder;

    public ResourceCompiler(ProjectBuilder builder, File aapt, boolean willBuildAppBundle, BuildProgressReceiver receiver) {
        this.willBuildAppBundle = willBuildAppBundle;
        aaptFile = aapt;
        progressReceiver = receiver;
        this.builder = builder;
    }
    public void compile() throws IOException, zy, MissingFileException {
        Compiler resourceCompiler = new Aapt2Compiler(builder, aaptFile, willBuildAppBundle);
        resourceCompiler.setProgressListener(new Compiler.ProgressListener() {
            @Override
            void onProgressUpdate(String newProgress, int step) {
                if (progressReceiver != null) {
                    progressReceiver.onProgress(newProgress, step);
                }
            }
        });
        resourceCompiler.compile();
    }
    
    interface Compiler {
        void compile() throws zy, MissingFileException;
        void setProgressListener(ProgressListener listener);

        abstract class ProgressListener {
            abstract void onProgressUpdate(String newProgress, int step);
        }
    }

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
            compiledBuiltInLibraryResourcesDirectory = new File(SketchApplication.getContext().getCacheDir(), "compiledLibs");
        }

        @Override
        public void compile() throws zy, MissingFileException {
            String outputPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            emptyOrCreateDirectory(outputPath);

            long startTime = System.currentTimeMillis();

            if (progressListener != null) {
                progressListener.onProgressUpdate("Compiling resources with AAPT2...", 9);
            }

            // Paralelizar compilações de bibliotecas
            compileLibrariesInParallel(outputPath);

            long savedTimeMillis = System.currentTimeMillis();
            compileProjectResources(outputPath);
            LogUtil.d(TAG + ":c", "Compiling project generated resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            savedTimeMillis = System.currentTimeMillis();
            compileImportedResources(outputPath);
            LogUtil.d(TAG + ":c", "Compiling project imported resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            savedTimeMillis = System.currentTimeMillis();
            link();
            LogUtil.d(TAG + ":c", "Linking resources took " + (System.currentTimeMillis() - savedTimeMillis) + " ms");

            LogUtil.d(TAG + ":c", "Total resource compilation took " + (System.currentTimeMillis() - startTime) + " ms");
        }

        private void compileLibrariesInParallel(String outputPath) throws zy, MissingFileException {
            compiledBuiltInLibraryResourcesDirectory.mkdirs();

            List<CompileTask> tasks = new ArrayList<>();

            // Built-in libraries
            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasResources()) {
                    String libName = library.getName();
                    String resPath = BuiltInLibraries.getLibraryResourcesPath(libName);
                    File outputFile = new File(compiledBuiltInLibraryResourcesDirectory, libName + ".zip");

                    compilingAssertDirectoryExists(resPath);

                    if (isBuiltInLibraryRecompilingNeeded(outputFile)) {
                        tasks.add(new CompileTask(aapt2, resPath, outputFile.getAbsolutePath(), "built-in library: " + libName));
                    } else {
                        LogUtil.d(TAG + ":cBILR", "Skipped recompilation for built-in library " + libName);
                    }
                }
            }

            // Local libraries
            for (String localResDir : buildHelper.mll.getResLocalLibrary()) {
                File localDir = new File(localResDir).getParentFile();
                if (localDir != null) {
                    compilingAssertDirectoryExists(localResDir);
                    String outputFileName = localDir.getName() + ".zip";
                    tasks.add(new CompileTask(aapt2, localResDir, outputPath + File.separator + outputFileName, "local library: " + localDir.getName()));
                }
            }

            if (tasks.isEmpty()) {
                LogUtil.d(TAG + ":cLLR", "No libraries to compile.");
                return;
            }

            int totalTasks = tasks.size();
            LogUtil.d(TAG + ":cLLR", "Compiling " + totalTasks + " libraries in parallel...");

            ExecutorService executor = Executors.newFixedThreadPool(Math.min(totalTasks, Runtime.getRuntime().availableProcessors()));
            CompletionService<String> completionService = new ExecutorCompletionService<>(executor);

            // Submeter todas as tarefas
            for (CompileTask task : tasks) {
                completionService.submit(task);
            }

            // Processar resultados conforme terminam
            int completed = 0;
            while (completed < totalTasks) {
                try {
                    Future<String> future = completionService.take();
                    String result = future.get();
                    if (result != null) {
                        throw new zy(result); // Erro de compilação
                    }
                    completed++;
                    LogUtil.d(TAG + ":cLLR", "Completed " + completed + "/" + totalTasks + " library compilations.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new zy("Compilation interrupted: " + e.getMessage());
                } catch (Exception e) {
                    throw new zy("Error during parallel compilation: " + e.getMessage());
                }
            }

            try {
                executor.shutdown();
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Tarefa de compilação encapsulada com Callable.
         */
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

                LogUtil.d(TAG + ":cTASK", "Compiling " + description + " -> " + outputPath);

                BinaryExecutor executor = new BinaryExecutor();
                executor.setCommands(commands);
                String log = executor.execute();
                if (!log.isEmpty()) {
                    LogUtil.e(TAG + ":cTASK", "Failed to compile " + description + ":\n" + log);
                    return log; // Retorna log de erro
                }
                return null; // Sucesso
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

            executeAndCheck(commands, "project resources");
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

                executeAndCheck(commands, "imported resources");
            }
        }

        private void executeAndCheck(ArrayList<String> commands, String context) throws zy {
            LogUtil.d(TAG + ":c", "Executing: " + commands);
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(commands);
            String log = executor.execute();
            if (!log.isEmpty()) {
                LogUtil.e(TAG, "Failed to compile " + context + ":\n" + log);
                throw new zy(log);
            }
        }

        private void link() throws zy, MissingFileException {
            String resourcesPath = buildHelper.yq.binDirectoryPath + File.separator + "res";
            if (progressListener != null)
                progressListener.onProgressUpdate("Linking resources with AAPT2...", 10);

            ArrayList<String> args = new ArrayList<>();
            args.add(aapt2.getAbsolutePath());
            args.add("link");
            if (buildAppBundle) args.add("--proto-format");
            args.add("--allow-reserved-package-id");
            args.add("--auto-add-overlay");
            args.add("--no-version-vectors");
            args.add("--no-version-transitions");

            args.add("--min-sdk-version");
            args.add(String.valueOf(buildHelper.settings.getMinSdkVersion()));
            args.add("--target-sdk-version");
            args.add(buildHelper.settings.getValue(ProjectSettings.SETTING_TARGET_SDK_VERSION, String.valueOf(VAR_DEFAULT_TARGET_SDK_VERSION)));

            args.add("--version-code");
            String versionCode = buildHelper.yq.versionCode;
            args.add((versionCode == null || versionCode.isEmpty()) ? "1" : versionCode);
            args.add("--version-name");
            String versionName = buildHelper.yq.versionName;
            args.add((versionName == null || versionName.isEmpty()) ? "1.0" : versionName);

            args.add("-I");
            String customAndroidSdk = buildHelper.build_settings.getValue(BuildSettings.SETTING_ANDROID_JAR_PATH, "");
            if (customAndroidSdk.isEmpty()) {
                args.add(buildHelper.androidJarPath);
            } else {
                linkingAssertFileExists(customAndroidSdk);
                args.add(customAndroidSdk);
            }

            linkingAssertDirectoryExists(buildHelper.yq.assetsPath);
            args.add("-A");
            args.add(buildHelper.yq.assetsPath);

            String importedAssetsPath = buildHelper.fpu.getPathAssets(buildHelper.yq.sc_id);
            if (FileUtil.isExistFile(importedAssetsPath)) {
                args.add("-A");
                args.add(importedAssetsPath);
            }

            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasAssets()) {
                    String assetsPath = BuiltInLibraries.getLibraryAssetsPath(library.getName());
                    linkingAssertDirectoryExists(assetsPath);
                    args.add("-A");
                    args.add(assetsPath);
                }
            }

            for (String localLibraryAssetsDirectory : new ManageLocalLibrary(buildHelper.yq.sc_id).getAssets()) {
                linkingAssertDirectoryExists(localLibraryAssetsDirectory);
                args.add("-A");
                args.add(localLibraryAssetsDirectory);
            }

            for (Jp library : buildHelper.builtInLibraryManager.getLibraries()) {
                if (library.hasResources()) {
                    args.add("-R");
                    args.add(new File(compiledBuiltInLibraryResourcesDirectory, library.getName() + ".zip").getAbsolutePath());
                }
            }

            File[] filesInCompiledResourcesPath = new File(resourcesPath).listFiles();
            if (filesInCompiledResourcesPath != null) {
                for (File file : filesInCompiledResourcesPath) {
                    if (file.isFile() && !file.getName().equals("project.zip") && !file.getName().equals("project-imported.zip")) {
                        args.add("-R");
                        args.add(file.getAbsolutePath());
                    }
                }
            }

            File projectArchive = new File(resourcesPath, "project.zip");
            if (projectArchive.exists()) {
                args.add("-R");
                args.add(projectArchive.getAbsolutePath());
            }

            File projectImportedArchive = new File(resourcesPath, "project-imported.zip");
            if (projectImportedArchive.exists()) {
                args.add("-R");
                args.add(projectImportedArchive.getAbsolutePath());
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
            if (!extraPackages.isEmpty()) {
                args.add("--extra-packages");
                args.add(extraPackages);
            }

            args.add("-o");
            args.add(buildHelper.yq.resourcesApkPath);

            LogUtil.d(TAG + ":l", args.toString());
            BinaryExecutor executor = new BinaryExecutor();
            executor.setCommands(args);
            String log = executor.execute();
            if (!log.isEmpty()) {
                LogUtil.e(TAG + ":l", log);
                throw new zy(log);
            }
        }

        private boolean isBuiltInLibraryRecompilingNeeded(File cachedCompiledResources) {
            if (cachedCompiledResources.exists()) {
                try {
                    Context context = SketchApplication.getContext();
                    return context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
                            .lastUpdateTime > cachedCompiledResources.lastModified();
                } catch (PackageManager.NameNotFoundException e) {
                    LogUtil.e(TAG + ":iBILRN", "Couldn't get package info: " + e.getMessage(), e);
                }
            }
            return true;
        }

        private void emptyOrCreateDirectory(String path) {
            if (FileUtil.isExistFile(path)) {
                FileUtil.deleteFile(path);
            }
            FileUtil.makeDir(path);
        }

        private void compilingAssertDirectoryExists(String directoryPath) throws MissingFileException {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                throw new MissingFileException(directory, MissingFileException.STEP_RESOURCE_COMPILING, true);
            }
        }

        public void linkingAssertFileExists(String filePath) throws MissingFileException {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new MissingFileException(file, MissingFileException.STEP_RESOURCE_LINKING, false);
            }
        }

        public void linkingAssertDirectoryExists(String filePath) throws MissingFileException {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new MissingFileException(file, MissingFileException.STEP_RESOURCE_LINKING, true);
            }
        }

        @Override
        public void setProgressListener(ProgressListener listener) {
            this.progressListener = listener;
        }
    }
}
