package mod.loddv.dev.tools.offlineTranslator;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public final class TranslatorUtils {

    private static final String OFFLINE_TRANSLATOR_PACKAGE = "dev.davidv.translator";

    private TranslatorUtils() {
    }

    /**
     * Opens Offline Translator with PROCESS_TEXT intent.
     *
     * @param context Activity context
     * @param textToTranslate Text to translate
     */
    public static void openOfflineTranslator(Context context, String textToTranslate) {
        try {
            Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, textToTranslate);

            try {
                intent.setPackage(OFFLINE_TRANSLATOR_PACKAGE);
                context.startActivity(intent);
            } catch (Exception e) {
                intent.setPackage(null);
                context.startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(context,
                    "Offline Translator is not installed. Install it from F-Droid to use translation.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks whether Offline Translator is installed.
     */
    public static boolean isOfflineTranslatorInstalled(Context context) {
        try {
            context.getPackageManager()
                    .getApplicationInfo(OFFLINE_TRANSLATOR_PACKAGE, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Opens Offline Translator on F-Droid.
     */
    public static void openOfflineTranslatorInFdroid(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                    "https://f-droid.org/packages/dev.davidv.translator/"
            ));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Could not open F-Droid",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
