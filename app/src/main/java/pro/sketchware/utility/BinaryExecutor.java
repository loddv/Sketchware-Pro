package pro.sketchware.utility;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class BinaryExecutor {

	private final ProcessBuilder processBuilder = new ProcessBuilder();

	public void setCommands(ArrayList<String> command) {
		this.processBuilder.command(command);
	}

	public String execute(long timeoutMillis) {
		Process process = null;
		StringBuilder outputBuilder = new StringBuilder();
		try {
			process = processBuilder.start();

			// Criar threads para captura simultânea de saída e erro
			StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), outputBuilder);
			StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), outputBuilder);
			Thread threadOut = new Thread(outputGobbler);
			Thread threadErr = new Thread(errorGobbler);
			threadOut.start();
			threadErr.start();


			boolean finished = process.waitFor(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
			if (! finished) {
				process.destroyForcibly(); // Abortando se passar o tempo limite
				throw new RuntimeException("Process timeout exceeded");
			}

			// Espera as threads acabarem
			threadOut.join();
			threadErr.join();

		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(new StringWriter()).append(outputBuilder));
		} finally {
			if (process != null) {
				process.destroy();
			}
		}

		return outputBuilder.toString();
	}

	public String getLog() {
		// Retorna o log se necessário
		return null;
	}

	private record StreamGobbler(InputStream inputStream,
	                             StringBuilder output) implements Runnable {

		@Override
		public void run() {
			try (Scanner scanner = new Scanner(inputStream)) {
				while (scanner.hasNextLine()) {
					output.append(scanner.nextLine()).append(System.lineSeparator());
				}
			}
		}
	}
}
