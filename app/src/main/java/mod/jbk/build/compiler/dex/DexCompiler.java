package mod.jbk.build.compiler.dex;

import android.annotation.SuppressLint;
import android.os.Build;

import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import a.a.a.ProjectBuilder;
import mod.hey.studios.project.ProjectSettings;

public class DexCompiler {
	@SuppressLint("ObsoleteSdkInt")
	public static void compileDexFiles(ProjectBuilder builder) throws CompilationFailedException {
		int minApiLevel;

		try {
			minApiLevel = Integer.parseInt(builder.settings.getValue(
					ProjectSettings.SETTING_MINIMUM_SDK_VERSION, "21"));
		} catch (NumberFormatException e) {
			throw new CompilationFailedException("Invalid minSdkVersion specified in Project Settings: " + e.getMessage());
		}

		if (! (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
			throw new IllegalStateException("Can't use d8 as API level " + Build.VERSION.SDK_INT + " < 26");
		}

		Collection<Path> programFiles;
		Path classesPath;
		if (builder.proguard.isShrinkingEnabled()) {
			classesPath = Paths.get(builder.yq.proguardClassesPath);
		} else {
			classesPath = Paths.get(builder.yq.compiledClassesPath);
		}

		// Use try-with-resources para garantir fechamento da stream e paralelize a listagem
		try (var stream = Files.walk(classesPath)) {
			programFiles = stream
					               .parallel()
					               .filter(Files::isRegularFile)
					               .filter(p -> p.getFileName().toString().endsWith(".class"))
					               .collect(Collectors.toList());
		} catch (IOException e) {
			throw new CompilationFailedException("Failed to list class files: " + e.getMessage());
		}

		if (programFiles.isEmpty()) {
			return; // Nada para compilar
		}

		// Processar biblioteca em paralelo, coletando apenas uma vez
		Collection<Path> libraryFiles = Arrays.stream(builder.getClasspath().split(":"))
				                                .parallel()
				                                .map(String::trim)
				                                .map(Paths::get)
				                                .filter(Files::exists)
				                                .filter(Files::isRegularFile)
				                                .collect(Collectors.toList());

		Path outputDir = Paths.get(builder.yq.binDirectoryPath, "dex");
		try {
			Files.createDirectories(outputDir);
		} catch (IOException e) {
			throw new CompilationFailedException("Failed to create output directory: " + e.getMessage());
		}

		D8.run(D8Command.builder()
				       .setMode(CompilationMode.RELEASE)
				       .setIntermediate(true)
				       .setMinApiLevel(minApiLevel)
				       .addLibraryFiles(libraryFiles)
				       .setOutput(outputDir, OutputMode.DexIndexed)
				       .addProgramFiles(programFiles)
				       .build());
	}
}
