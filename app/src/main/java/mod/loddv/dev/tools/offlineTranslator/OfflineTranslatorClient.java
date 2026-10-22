package mod.loddv.dev.tools.offlineTranslator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import mod.loddv.dev.tools.offlineTranslator.translator.ITranslationCallback;
import mod.loddv.dev.tools.offlineTranslator.translator.ITranslationService;
import mod.loddv.dev.tools.offlineTranslator.translator.TranslationError;

public class OfflineTranslatorClient {

    private static final String TAG = "OfflineTranslatorClient";
    private static final String TRANSLATOR_PACKAGE = "dev.davidv.translator";
    private static final String TRANSLATION_SERVICE_ACTION = "dev.davidv.translator.ITranslationService";

    private final Context context;
    private ITranslationService translationService;
    private boolean isBound = false;
    private final CountDownLatch connectLatch = new CountDownLatch(1);

    public interface Callback {
        void onSuccess(String translatedText);

        void onError(TranslationError error);
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to Offline Translator AIDL service");
            translationService = ITranslationService.Stub.asInterface(service);
            isBound = true;
            connectLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from Offline Translator AIDL service");
            isBound = false;
            translationService = null;
        }
    };

    public OfflineTranslatorClient(Context context) {
        this.context = Objects.requireNonNull(context, "context").getApplicationContext();
    }

    public boolean connect() {
        try {
            Intent intent = new Intent(TRANSLATION_SERVICE_ACTION);
            intent.setPackage(TRANSLATOR_PACKAGE);
            boolean success = context.bindService(
                    intent,
                    connection,
                    Context.BIND_AUTO_CREATE
            );
            if (!success) {
                Log.e(TAG, "Failed to bind Offline Translator service");
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error binding Offline Translator service", e);
            return false;
        }
    }

    public boolean connectAndWait(long timeoutMs) {
        if (connect()) {
            try {
                return connectLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Interrupted while waiting for connection", e);
            }
        }
        return false;
    }

    public void disconnect() {
        if (isBound) {
            try {
                context.unbindService(connection);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding service", e);
            } finally {
                isBound = false;
                translationService = null;
            }
        }
    }

    public void translate(String textToTranslate, String sourceLanguage, String targetLanguage, Callback callback) throws Exception {
        if (!isBound || translationService == null) {
            throw new IllegalStateException("Offline Translator service is not connected");
        }
        translationService.translate(
                textToTranslate,
                ("auto".equalsIgnoreCase(sourceLanguage) || sourceLanguage == null || sourceLanguage.trim().isEmpty()) ? null : sourceLanguage,
                targetLanguage,
                new ITranslationCallback.Stub() {
                    @Override
                    public void onTranslationResult(String translatedText) {
                        if (callback != null) {
                            callback.onSuccess(translatedText);
                        }
                    }

                    @Override
                    public void onTranslationError(TranslationError error) {
                        if (callback != null) {
                            callback.onError(error);
                        }
                    }
                }
        );
    }

    public String translateSync(String textToTranslate, String sourceLanguage, String targetLanguage, long timeoutMs) throws Exception {
        if (!isBound || translationService == null) {
            throw new IllegalStateException("Offline Translator service is not connected");
        }
        CountDownLatch latch = new CountDownLatch(1);
        final String[] result = new String[1];
        final Exception[] errorHolder = new Exception[1];
        translate(textToTranslate, sourceLanguage, targetLanguage, new Callback() {
            @Override
            public void onSuccess(String translatedText) {
                result[0] = translatedText;
                latch.countDown();
            }

            @Override
            public void onError(TranslationError error) {
                String msg = error != null ? (error.getType() + (error.getMessage() != null ?
                        ": " + error.getMessage() : "")) : "Translation error";
                errorHolder[0] = new Exception(msg);
                latch.countDown();
            }
        });
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new Exception("Translation timed out after " + timeoutMs + "ms");
        }
        if (errorHolder[0] != null) {
            throw errorHolder[0];
        }
        return result[0];
    }

    public boolean isConnected() {
        return isBound && translationService != null;
    }

    public static boolean isOfflineTranslatorInstalled(Context context) {
        try {
            context.getPackageManager()
                    .getApplicationInfo(TRANSLATOR_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
