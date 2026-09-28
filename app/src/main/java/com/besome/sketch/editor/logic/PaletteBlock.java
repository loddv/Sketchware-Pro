package com.besome.sketch.editor.logic;

import static pro.sketchware.utility.ThemeUtils.getColor;
import static pro.sketchware.utility.ThemeUtils.isDarkThemeEnabled;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;

import a.a.a.Rs;
import a.a.a.Ts;
import a.a.a.wB;
import mod.hilal.saif.activities.tools.ConfigActivity;
import pro.sketchware.R;
import pro.sketchware.databinding.PaletteBlockBinding;
import pro.sketchware.databinding.PaletteBlockHorizontalBinding;

public class PaletteBlock extends LinearLayout {

    private float f = 0.0F;
    private Context context;

    // Um dos dois será null dependendo do modo
    @Nullable
    private PaletteBlockBinding bindingVertical;
    @Nullable
    private PaletteBlockHorizontalBinding bindingHorizontal;

    private boolean isHorizontalMode;
    private boolean needsReinitialization = false;

    @Nullable
    private OnPaletteModeChangedListener modeChangeListener;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    public PaletteBlock(Context context) {
        super(context);
        initialize(context, null);
    }

    public PaletteBlock(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    /**
     * Interface para notificar quando o modo da paleta muda
     */
    public interface OnPaletteModeChangedListener {
        /**
         * Chamado quando o modo da paleta é alterado
         * @param isHorizontalMode true se modo horizontal, false se vertical
         */
        void onPaletteModeChanged(boolean isHorizontalMode);
    }

    /**
     * Define o listener que será notificado quando o modo mudar
     */
    public void setOnPaletteModeChangedListener(@Nullable OnPaletteModeChangedListener listener) {
        this.modeChangeListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startMonitoringSettings();
        if (needsReinitialization) {
            reinitialize();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopMonitoringSettings();
    }

    private void initialize(Context context, AttributeSet attrs) {
        this.context = context;
        this.isHorizontalMode = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL);
        f = wB.a(context, 1.0F);
        LayoutInflater inflater = LayoutInflater.from(context);
        if (isHorizontalMode) {
            bindingHorizontal = PaletteBlockHorizontalBinding.inflate(inflater, this, true);
        } else {
            bindingVertical = PaletteBlockBinding.inflate(inflater, this, true);
        }
    }

    private void reinitialize() {
        boolean newIsHorizontalMode = ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_PALETTE_ON_VERTICAL);
        if (newIsHorizontalMode != this.isHorizontalMode) {
            this.isHorizontalMode = newIsHorizontalMode;
            removeAllViews();
            initialize(context, null);
            // Notificar o listener sobre a mudança de modo
            notifyModeChanged();
            // O parent (LogicEditor) precisa repopular a paleta com blocos
        }
        needsReinitialization = false;
    }

    /**
     * Notifica o listener que o modo da paleta foi alterado
     */
    private void notifyModeChanged() {
        if (modeChangeListener != null) {
            modeChangeListener.onPaletteModeChanged(isHorizontalMode);
        }
    }

    /**
     * Inicia monitoramento das mudanças nas preferências compartilhadas
     * Detecta automaticamente quando SETTING_PALETTE_ON_VERTICAL é alterado
     */
    private void startMonitoringSettings() {
        if (preferenceChangeListener == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            preferenceChangeListener = (pref, key) -> {
                if (ConfigActivity.SETTING_PALETTE_ON_VERTICAL.equals(key)) {
                    needsReinitialization = true;
                    if (isAttachedToWindow()) {
                        reinitialize();
                    }
                }
            };
            prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    /**
     * Para monitoramento das mudanças nas preferências
     */
    private void stopMonitoringSettings() {
        if (preferenceChangeListener != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            preferenceChangeListener = null;
        }
    }

    // Métodos auxiliares para acessar os containers corretos
    private LinearLayout getBlockBuilder() {
        return isHorizontalMode ? bindingHorizontal.blockBuilder : bindingVertical.blockBuilder;
    }

    private LinearLayout getActionsContainer() {
        return isHorizontalMode ? bindingHorizontal.actionsContainer : bindingVertical.actionsContainer;
    }

    private View getScrollView() {
        return isHorizontalMode ? bindingHorizontal.scrollHorizontal : bindingVertical.scroll;
    }

    public Ts a(String var1, String var2, String var3) {
        View spacer = new View(context);
        spacer.setLayoutParams(getLayoutParams(8.0F));
        getBlockBuilder().addView(spacer);
        Rs blockView = new Rs(context, -1, var1, var2, var3);
        blockView.setContentDescription(generateContentDescription(var3));
        blockView.setBlockType(1);
        getBlockBuilder().addView(blockView);
        return blockView;
    }

    public Ts a(String var1, String var2, String var3, String var4) {
        View spacer = new View(context);
        spacer.setLayoutParams(getLayoutParams(8.0F));
        getBlockBuilder().addView(spacer);
        Rs blockView = new Rs(context, -1, var1, var2, var3, var4);
        blockView.setContentDescription(generateContentDescription(var4));
        blockView.setBlockType(1);
        getBlockBuilder().addView(blockView);
        return blockView;
    }

    public TextView a(String title) {
        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextSize(10.0F);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding((int) (f * 8.0F), 0, (int) (f * 8.0F), 0);
        MaterialCardView cardView = new MaterialCardView(context);
        LinearLayout.LayoutParams params = getLayoutParams(30.0F);
        params.setMargins(0, 0, (int) (f * 4), (int) (f * 6));
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(getColor(context,
                isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceContainerHighest));
        cardView.addView(textView);
        getActionsContainer().addView(cardView);
        return textView;
    }

    public void a() {
        getBlockBuilder().removeAllViews();
        getActionsContainer().removeAllViews();
    }

    public void a(String title, int color) {
        MaterialCardView cardView = new MaterialCardView(context);
        LinearLayout.LayoutParams params = getLayoutParams(18.0F);
        params.topMargin = (int) (f * 16.0F);
        cardView.setLayoutParams(params);
        cardView.setCardBackgroundColor(color);
        cardView.setRadius(f * 8f);
        TextView textView = new TextView(context);
        textView.setText(title);
        textView.setTextColor(getColor(context,
                isDarkThemeEnabled(context) ? R.attr.colorOnSurface : R.attr.colorOnSurfaceInverse));
        textView.setTextSize(10.0F);
        textView.setGravity(Gravity.CENTER | Gravity.LEFT);
        textView.setPadding((int) (f * 12.0F), 0, (int) (f * 12.0F), 0);
        cardView.addView(textView);
        getBlockBuilder().addView(cardView);
    }

    public void addDeprecatedBlock(String message, String type, String opCode) {
        if (message != null && !message.isEmpty()) {
            a(message, getColor(context,
                    isDarkThemeEnabled(context) ? R.attr.colorSurfaceContainerHigh : R.attr.colorSurfaceInverse));
        }
        Ts blockView = a("", type, opCode);
        blockView.e = 0xFFBDBDBD;
        blockView.setTag(opCode);
    }

    private String generateContentDescription(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            char current = name.charAt(i);
            char previous = name.charAt(i - 1);
            if (Character.isUpperCase(current)) {
                if (Character.isLowerCase(previous) || (i + 1 < name.length() && Character.isLowerCase(name.charAt(i + 1)))) {
                    result.append(' ');
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private LinearLayout.LayoutParams getLayoutParams(float heightMultiplier) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (f * heightMultiplier)
        );
    }

    public void setDragEnabled(boolean dragEnabled) {
        if (isHorizontalMode) {
            if (dragEnabled) {
                assert bindingHorizontal != null;
                bindingHorizontal.scroll.b();
                bindingHorizontal.scrollHorizontal.b();
            } else {
                assert bindingHorizontal != null;
                bindingHorizontal.scroll.a();
                bindingHorizontal.scrollHorizontal.a();
            }
        } else {
            if (dragEnabled) {
                assert bindingVertical != null;
                bindingVertical.scroll.b();
                bindingVertical.scrollHorizontal.b();
            } else {
                assert bindingVertical != null;
                bindingVertical.scroll.a();
                bindingVertical.scrollHorizontal.a();
            }
        }
    }

    public void setMinWidth(int minWidth) {
        if (isHorizontalMode) {
            assert bindingHorizontal != null;
            bindingHorizontal.scroll.setMinimumWidth(minWidth - (int) (f * 5.0F));
            bindingHorizontal.scrollHorizontal.setMinimumWidth(minWidth - (int) (f * 5.0F));
            getLayoutParams().width = minWidth;
        } else {
            assert bindingVertical != null;
            bindingVertical.scroll.setMinimumWidth(minWidth - (int) (f * 5.0F));
            bindingVertical.scrollHorizontal.setMinimumWidth(minWidth - (int) (f * 5.0F));
            getLayoutParams().width = minWidth;
        }
    }

    /*public void setUseScroll(boolean useScroll) {
        if (isHorizontalMode) {
            if (bindingHorizontal != null) bindingHorizontal.scrollHorizontal.setUseScroll(useScroll);
        } else {
            if (bindingVertical != null) bindingVertical.scroll.setUseScroll(useScroll);
        }
    }*/
    public void setUseScroll(boolean useScroll) {
        if (isHorizontalMode) {
            assert bindingHorizontal != null;
            bindingHorizontal.scroll.setUseScroll(useScroll);
            bindingHorizontal.scrollHorizontal.setUseScroll(useScroll);
        } else {
            assert bindingVertical != null;
            bindingVertical.scroll.setUseScroll(useScroll);
            bindingVertical.scrollHorizontal.setUseScroll(useScroll);
        }
    }
}
