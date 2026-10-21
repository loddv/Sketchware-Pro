package mod.loddv.dev.tools.offlineTranslator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BatchTranslatorManager {

    private final OfflineTranslator translator;
    private final int batchSize;

    public BatchTranslatorManager(OfflineTranslator translator, int batchSize) {
        this.translator = Objects.requireNonNull(translator, "translator");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        this.batchSize = batchSize;
    }

    public List<String> translate(List<String> texts) throws Exception {
        Objects.requireNonNull(texts, "texts");
        if (texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> translations = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, texts.size());
            List<String> batch = Collections.unmodifiableList(
                    new ArrayList<>(texts.subList(start, end)));
            List<String> translatedBatch = translator.translate(batch);
            if (translatedBatch == null || translatedBatch.size() != batch.size()) {
                throw new IllegalStateException(
                        "Translator must return exactly one result for each input in a batch");
            }
            for (String translation : translatedBatch) {
                translations.add(Objects.requireNonNull(
                        translation, "Translator returned a null result"));
            }
        }
        return translations;
    }

    @FunctionalInterface
    public interface OfflineTranslator {
        List<String> translate(List<String> texts) throws Exception;
    }
}
