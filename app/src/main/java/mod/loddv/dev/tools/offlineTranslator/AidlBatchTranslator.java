package mod.loddv.dev.tools.offlineTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AidlBatchTranslator {

    private final OfflineTranslatorClient client;
    private final String sourceLanguage;
    private final String targetLanguage;

    public AidlBatchTranslator(OfflineTranslatorClient client, String sourceLanguage, String targetLanguage) {
        this.client = Objects.requireNonNull(client, "client");
        this.sourceLanguage = Objects.requireNonNull(sourceLanguage, "sourceLanguage");
        this.targetLanguage = Objects.requireNonNull(targetLanguage, "targetLanguage");
    }

    public List<String> translate(List<String> texts) throws Exception {
        Objects.requireNonNull(texts, "texts");
        List<String> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            if (text == null) {
                results.add(null);
                continue;
            }
            String translated = client.translateSync(text, sourceLanguage, targetLanguage, 15000);
            results.add(translated != null ? translated : text);
        }
        return results;
    }
}
