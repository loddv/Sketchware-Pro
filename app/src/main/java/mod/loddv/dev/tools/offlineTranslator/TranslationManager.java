package mod.loddv.dev.tools.offlineTranslator;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.Objects;

import mod.loddv.dev.tools.offlineTranslator.translator.TranslationError;

public class TranslationManager {

    private static final String TAG = "TranslationManager";

    private final Context context;
    private final OfflineTranslatorClient aidlClient;

    public interface TranslationCallback {
        void onSuccess(String translation);

        void onError(String error);
    }

    public TranslationManager(Context context) {
        this.context = Objects.requireNonNull(context, "context").getApplicationContext();
        if (OfflineTranslatorClient.isOfflineTranslatorInstalled(this.context)) {
            this.aidlClient = new OfflineTranslatorClient(this.context);
            this.aidlClient.connect();
        } else {
            this.aidlClient = null;
        }
    }

    /**
     * Translates text with automatic AIDL to Intent fallback.
     */
    public void translate(
            String text,
            String sourceLang,
            String targetLang,
            TranslationCallback callback) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(callback, "callback");
        if (aidlClient != null && aidlClient.isConnected()) {
            Log.d(TAG, "Using AIDL Service for translation");
            try {
                aidlClient.translate(text, sourceLang, targetLang, new OfflineTranslatorClient.Callback() {
                    @Override
                    public void onSuccess(String translatedText) {
                        callback.onSuccess(translatedText != null ? translatedText : text);
                    }

                    @Override
                    public void onError(TranslationError error) {
                        String msg = error != null ? (error.getType() + (error.getMessage() != null ?
                                ": " + error.getMessage() : "")) : "Translation error";
                        Log.e(TAG, "AIDL translation error: " + msg);
                        fallbackToIntent(text, callback);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "AIDL translation call failed: " + e.getMessage(), e);
                fallbackToIntent(text, callback);
            }
        } else {
            Log.d(TAG, "Offline Translator service not connected, falling back to Intent");
            fallbackToIntent(text, callback);
        }
    }

    /**
     * Translates a Sketchware project strings file using AIDL service.
     */
    public File translateProjectStrings(File projectDirectory, String sourceLang, String targetLang,
                                        String localeQualifier) throws Exception {
        if (aidlClient == null || !aidlClient.isConnected()) {
            throw new IllegalStateException("Offline Translator AIDL service is not connected");
        }
        AidlBatchTranslator batchTranslator = new AidlBatchTranslator(aidlClient, sourceLang, targetLang);
        SketchwareProjectTranslator projectTranslator = new SketchwareProjectTranslator(batchTranslator);
        return projectTranslator.translateStrings(projectDirectory, localeQualifier);
    }

    private void fallbackToIntent(String text, TranslationCallback callback) {
        try {
            TranslatorUtils.openOfflineTranslator(context, text);
            callback.onSuccess("Opened Offline Translator");
        } catch (Exception e) {
            callback.onError("Offline Translator is not installed or could not be opened");
        }
    }

    public void disconnect() {
        if (aidlClient != null) {
            aidlClient.disconnect();
        }
    }

    public boolean isAidlConnected() {
        return aidlClient != null && aidlClient.isConnected();
    }

    public OfflineTranslatorClient getAidlClient() {
        return aidlClient;
    }

    public String getAvailableLanguages() throws Exception {
        return null;
    }
}
