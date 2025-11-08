package com.besome.sketch.projects;

import static mod.hey.studios.util.ProjectFile.getDefaultColor;

import android.graphics.Color;

import java.util.Random;

import mod.hey.studios.util.ProjectFile;


public class ThemeManager {

	private static final ThemePreset[] THEME_PRESETS = {
			// Previous themes (omitted for brevity, including all from the prior response)
			new ThemePreset("Material Purple",
					0xFF03DAC5, 0xFF6200EE, 0xFF3700B3, 0xFFE8EAF6, 0xFFBDBDBD),
			new ThemePreset("Dark Material Purple",
					0xFFBB86FC, 0xFF3700B3, 0xFF270084, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Blue",
					0xFF03DAC5, 0xFF1976D2, 0xFF0D47A1, 0xFFE3F2FD, 0xFFBDBDBD),
			new ThemePreset("Dark Material Blue",
					0xFF64B5F6, 0xFF0D47A1, 0xFF062152, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Green",
					0xFF03DAC5, 0xFF388E3C, 0xFF1B5E20, 0xFFE8F5E8, 0xFFBDBDBD),
			new ThemePreset("Dark Material Green",
					0xFF81C784, 0xFF1B5E20, 0xFF0C2C0E, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Red",
					0xFF03DAC5, 0xFFD32F2F, 0xFFB71C1C, 0xFFFFEBEE, 0xFFBDBDBD),
			new ThemePreset("Dark Material Red",
					0xFFEF9A9A, 0xFFB71C1C, 0xFF7F1313, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Orange",
					0xFF03DAC5, 0xFFF57C00, 0xFFE65100, 0xFFFFF3E0, 0xFFBDBDBD),
			new ThemePreset("Dark Material Orange",
					0xFFFFCC80, 0xFFE65100, 0xFF9E3800, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Teal",
					0xFF03DAC5, 0xFF00796B, 0xFF004D40, 0xFFE0F2F1, 0xFFBDBDBD),
			new ThemePreset("Dark Material Teal",
					0xFF4DB6AC, 0xFF004D40, 0xFF002922, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Indigo",
					0xFF03DAC5, 0xFF3F51B5, 0xFF1A237E, 0xFFE8EAF6, 0xFFBDBDBD),
			new ThemePreset("Dark Material Indigo",
					0xFF9FA8DA, 0xFF1A237E, 0xFF0D1241, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Pink",
					0xFF03DAC5, 0xFFC2185B, 0xFF880E4F, 0xFFFCE4EC, 0xFFBDBDBD),
			new ThemePreset("Dark Material Pink",
					0xFFF48FB1, 0xFF880E4F, 0xFF580933, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Dark Purple",
					0xFF03DAC5, 0xFFBB86FC, 0xFF3700B3, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Dark Blue",
					0xFF03DAC5, 0xFF64B5F6, 0xFF1976D2, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Light Purple",
					0xFF03DAC5, 0xFF6200EE, 0xFF3700B3, 0xFFF3E5F5, 0xFFE1BEE7),
			new ThemePreset("Light Blue",
					0xFF03DAC5, 0xFF1976D2, 0xFF0D47A1, 0xFFE3F2FD, 0xFFBBDEFB),
			new ThemePreset("Material Brown",
					0xFF03DAC5, 0xFF795548, 0xFF4E342E, 0xFFEFEBE9, 0xFFBDBDBD),
			new ThemePreset("Dark Material Brown",
					0xFFA1887F, 0xFF4E342E, 0xFF2C1D19, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Grey",
					0xFF03DAC5, 0xFF616161, 0xFF212121, 0xFFFAFAFA, 0xFFBDBDBD),
			new ThemePreset("Dark Material Grey",
					0xFFBDBDBD, 0xFF212121, 0xFF000000, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Dark Teal",
					0xFF80CBC4, 0xFF009688, 0xFF00796B, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Dark Orange",
					0xFFFFAB91, 0xFFFF5722, 0xFFE64A19, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("AMOLED",
					0xFF2962FF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF2962FF, 0xFF424242),
			new ThemePreset("Dark AMOLED",
					0xFF2962FF, 0xFF000000, 0xFF000000, 0xFF2962FF, 0xFF424242),
			new ThemePreset("Material Cyan",
					0xFF18FFFF, 0xFF00BCD4, 0xFF0097A7, 0xFFE0F7FA, 0xFFBDBDBD),
			new ThemePreset("Dark Material Cyan",
					0xFF4DD0E1, 0xFF0097A7, 0xFF006470, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Yellow",
					0xFF03DAC5, 0xFFFBC02D, 0xFFF57F17, 0xFFFFFDE7, 0xFFBDBDBD),
			new ThemePreset("Dark Material Yellow",
					0xFFFFE082, 0xFFF57F17, 0xFF8B4F00, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Lime",
					0xFF03DAC5, 0xFFCDDC39, 0xFFAFB42B, 0xFFF9FBE7, 0xFFBDBDBD),
			new ThemePreset("Dark Material Lime",
					0xFFE6EE9C, 0xFFAFB42B, 0xFF6B6D1A, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Deep Purple",
					0xFF03DAC5, 0xFF673AB7, 0xFF512DA8, 0xFFEDE7F6, 0xFFBDBDBD),
			new ThemePreset("Dark Material Deep Purple",
					0xFFB39DDB, 0xFF512DA8, 0xFF311B92, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Material Amber",
					0xFF03DAC5, 0xFFFFA000, 0xFFFF6F00, 0xFFFFF8E1, 0xFFBDBDBD),
			new ThemePreset("Dark Material Amber",
					0xFFFFD180, 0xFFFF6F00, 0xFF8F3F00, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Light Green",
					0xFF03DAC5, 0xFF388E3C, 0xFF1B5E20, 0xFFE8F5E8, 0xFFA5D6A7),
			new ThemePreset("Light Red",
					0xFF03DAC5, 0xFFD32F2F, 0xFFB71C1C, 0xFFFFEBEE, 0xFFEF9A9A),
			new ThemePreset("Light Teal",
					0xFF03DAC5, 0xFF00796B, 0xFF004D40, 0xFFE0F2F1, 0xFF80CBC4),
			new ThemePreset("Light Indigo",
					0xFF03DAC5, 0xFF3F51B5, 0xFF1A237E, 0xFFE8EAF6, 0xFF9FA8DA),
			new ThemePreset("Light Pink",
					0xFF03DAC5, 0xFFC2185B, 0xFF880E4F, 0xFFFCE4EC, 0xFFF48FB1),
			new ThemePreset("Dark Cyan",
					0xFF18FFFF, 0xFF00BCD4, 0xFF0097A7, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("Monochrome Light",
					0xFFB0BEC5, 0xFF78909C, 0xFF455A64, 0xFFFAFAFA, 0xFFECEFF1),
			new ThemePreset("Monochrome Dark",
					0xFFB0BEC5, 0xFF455A64, 0xFF263238, 0xFF1F1F1F, 0xFF666666),
			new ThemePreset("High Contrast Light",
					0xFFFF4081, 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFFB0BEC5),
			new ThemePreset("High Contrast Dark",
					0xFFFF4081, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF424242),

			// New themes and variations
			// Light and Dark Blue Grey
			new ThemePreset("Material Blue Grey",
					0xFF03DAC5, 0xFF607D8B, 0xFF455A64, 0xFFECEFF1, 0xFFBDBDBD),
			new ThemePreset("Dark Material Blue Grey",
					0xFF90A4AE, 0xFF455A64, 0xFF263238, 0xFF1F1F1F, 0xFF666666),

			// Light and Dark Deep Orange
			new ThemePreset("Material Deep Orange",
					0xFF03DAC5, 0xFFFF5722, 0xFFE64A19, 0xFFFFE0B2, 0xFFBDBDBD),
			new ThemePreset("Dark Material Deep Orange",
					0xFFFF8A65, 0xFFE64A19, 0xFFBF360C, 0xFF1F1F1F, 0xFF666666),

			// Light and Dark Light Green
			new ThemePreset("Material Light Green",
					0xFF03DAC5, 0xFF8BC34A, 0xFF689F38, 0xFFF1F8E9, 0xFFBDBDBD),
			new ThemePreset("Dark Material Light Green",
					0xFFAED581, 0xFF689F38, 0xFF33691E, 0xFF1F1F1F, 0xFF666666),

			// Solarized Light
			new ThemePreset("Solarized Light",
					0xFF2AA198, 0xFF268BD2, 0xFF073642, 0xFFFDF6E3, 0xFFEEE8D5),

			// Solarized Dark
			new ThemePreset("Solarized Dark",
					0xFF2AA198, 0xFF268BD2, 0xFF073642, 0xFF002B36, 0xFF586E75),

			// Pastel Light
			new ThemePreset("Pastel Light",
					0xFF80DEEA, 0xFF64B5F6, 0xFF4FC3F7, 0xFFE1F5FE, 0xFFB3E5FC),

			// Pastel Dark
			new ThemePreset("Pastel Dark",
					0xFF80DEEA, 0xFF4FC3F7, 0xFF0288D1, 0xFF1F1F1F, 0xFF4F5B62),

			// Nature Forest
			new ThemePreset("Nature Forest",
					0xFF8D6E63, 0xFF4CAF50, 0xFF2E7D32, 0xFFE8F5E9, 0xFFA5D6A7),

			// Dark Nature Forest
			new ThemePreset("Dark Nature Forest",
					0xFF8D6E63, 0xFF2E7D32, 0xFF1B5E20, 0xFF1F1F1F, 0xFF4CAF50),

			// Ocean Breeze
			new ThemePreset("Ocean Breeze",
					0xFF26A69A, 0xFF0288D1, 0xFF01579B, 0xFFE3F2FD, 0xFF4FC3F7),

			// Dark Ocean Breeze
			new ThemePreset("Dark Ocean Breeze",
					0xFF26A69A, 0xFF01579B, 0xFF003087, 0xFF1F1F1F, 0xFF4F5B62),

			// Light Brown
			new ThemePreset("Light Brown",
					0xFF03DAC5, 0xFF795548, 0xFF4E342E, 0xFFEFEBE9, 0xFFA1887F),

			// Light Grey
			new ThemePreset("Light Grey",
					0xFF03DAC5, 0xFF616161, 0xFF212121, 0xFFFAFAFA, 0xFFE0E0E0),

			// Warm Sunset
			new ThemePreset("Warm Sunset",
					0xFFFF7043, 0xFFF44336, 0xFFD81B60, 0xFFFFEBEE, 0xFFFFB300),

			// Dark Warm Sunset
			new ThemePreset("Dark Warm Sunset",
					0xFFFF7043, 0xFFD81B60, 0xFFAD1457, 0xFF1F1F1F, 0xFFEF5350)
	};

	public static ThemePreset[] getThemePresets() {
		return THEME_PRESETS;
	}

	public static ThemePreset generateRandomTheme() {
		Random random = new Random();

		int primaryColor = Color.HSVToColor(new float[]{
				random.nextFloat() * 360,
				0.6f + random.nextFloat() * 0.3f,
				0.5f + random.nextFloat() * 0.4f
		});

		int primaryDarkColor = darkenColor(primaryColor, 0.3f);

		float[] hsv = new float[3];
		Color.colorToHSV(primaryColor, hsv);
		hsv[0] = (hsv[0] + 180) % 360;
		hsv[1] = 0.7f + random.nextFloat() * 0.2f;
		hsv[2] = 0.8f + random.nextFloat() * 0.2f;
		int accentColor = Color.HSVToColor(hsv);

		int controlHighlightColor = lightenColor(primaryColor, 0.9f);
		int controlNormalColor = Color.GRAY;

		return new ThemePreset("Random Theme", accentColor, primaryColor, primaryDarkColor,
				controlHighlightColor, controlNormalColor);
	}

	public static ThemePreset getDefault() {
		return new ThemePreset(
				"Default",
				getDefaultColor(ProjectFile.COLOR_ACCENT),
				getDefaultColor(ProjectFile.COLOR_PRIMARY),
				getDefaultColor(ProjectFile.COLOR_PRIMARY_DARK),
				getDefaultColor(ProjectFile.COLOR_CONTROL_HIGHLIGHT),
				getDefaultColor(ProjectFile.COLOR_CONTROL_NORMAL));
	}

	private static int darkenColor(int color, float factor) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] *= (1 - factor);
		return Color.HSVToColor(hsv);
	}

	private static int lightenColor(int color, float factor) {
		float[] hsv = new float[3];
		Color.colorToHSV(color, hsv);
		hsv[2] = Math.min(1.0f, hsv[2] + factor);
		hsv[1] = Math.max(0.0f, hsv[1] - factor * 0.5f);
		return Color.HSVToColor(hsv);
	}

	public static class ThemePreset {
		public String name;
		public int colorAccent;
		public int colorPrimary;
		public int colorPrimaryDark;
		public int colorControlHighlight;
		public int colorControlNormal;

		public ThemePreset(String name, int colorAccent, int colorPrimary, int colorPrimaryDark,
		                   int colorControlHighlight, int colorControlNormal) {
			this.name = name;
			this.colorAccent = colorAccent;
			this.colorPrimary = colorPrimary;
			this.colorPrimaryDark = colorPrimaryDark;
			this.colorControlHighlight = colorControlHighlight;
			this.colorControlNormal = colorControlNormal;
		}
	}

} 