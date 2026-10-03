package pro.sketchware.lib.validator;

import android.content.Context;

import com.google.android.material.textfield.TextInputLayout;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import a.a.a.MB;

public class VariableModifierValidator extends MB {
    public static final Pattern PATTERN_MODIFIER = Pattern.compile(
            "\\b(public|protected|private|static|final|transient|volatile)\\b"
    );

    public VariableModifierValidator(Context context, TextInputLayout textInputLayout) {
        super(context, textInputLayout);
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int n, int n2, int n3) {
        String input = charSequence.toString();
        String trimmedInput = input.trim();
        // Se o campo estiver vazio ou tiver apenas espaços
        if (trimmedInput.isEmpty()) {
            b.setError(null);
            d = true;
            return;
        }
        String[] words = trimmedInput.split("\\s+");
        String reconsInput = String.join(" ", words);
        // Permite um espaço único no final enquanto o usuário digita,
        // mas bloqueia múltiplos espaços consecutivos ou espaços no meio das palavras.
        boolean hasSingleTrailingSpace = input.endsWith(" ") && !input.endsWith("  ");
        String expectedInput = reconsInput + (hasSingleTrailingSpace ? " " : "");
        if (!input.equals(expectedInput)) {
            b.setError("Extra spaces between words are not allowed.");
            d = false;
            return;
        }
        Set<String> usedModifiers = new HashSet<>();
        boolean hasAccessModifier = false;
        for (String word : words) {
            if (!PATTERN_MODIFIER.matcher(word).matches()) {
                b.setError("Invalid modifier: " + word);
                d = false;
                return;
            }
            if (!usedModifiers.add(word)) {
                b.setError("Duplicate modifier: " + word);
                d = false;
                return;
            }
            if (isAccessModifier(word)) {
                if (hasAccessModifier) {
                    b.setError("Access modifier can only set one of public / protected / private");
                    d = false;
                    return;
                }
                hasAccessModifier = true;
            }
        }
        b.setError(null);
        d = true;
    }

    private boolean isAccessModifier(String word) {
        return word.equals("public") || word.equals("protected") || word.equals("private");
    }

    public boolean isValid() {
        return b();
    }
}
