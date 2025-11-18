package mod.hilal.saif.activities.tools;

import static pro.sketchware.utility.GsonUtils.getGson;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.besome.sketch.lib.base.BaseAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import dev.pranav.filepicker.FilePickerCallback;
import dev.pranav.filepicker.FilePickerDialogFragment;
import dev.pranav.filepicker.FilePickerOptions;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;

public class BlocksManagerDetailsActivity extends BaseAppCompatActivity {

	private static final String BLOCK_EXPORT_PATH = new File(FileUtil.getExternalStorageDir(), ".sketchware/resources/block/export/").getAbsolutePath();

	private final ArrayList<HashMap<String, Object>> filtered_list = new ArrayList<>();
	private final ArrayList<Integer> reference_list = new ArrayList<>();
	private ArrayList<HashMap<String, Object>> all_blocks_list = new ArrayList<>();
	private String blocks_path = "";
	private String mode = "normal";
	private ArrayList<HashMap<String, Object>> pallet_list = new ArrayList<>();
	private String pallet_path = "";
	private int palette = 0;
	private Parcelable listViewSavedState;

	private Toolbar toolbar;
	private ListView block_list;
	private LinearLayout background;
	private com.google.android.material.floatingactionbutton.FloatingActionButton fab_button;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blocks_manager_details);

		background = findViewById(R.id.background);
		block_list = findViewById(R.id.block_list);
		fab_button = findViewById(R.id.fab_button);

		initialize();
		_receive_intents();
	}

	private void initialize() {

		toolbar = (Toolbar) getLayoutInflater().inflate(R.layout.toolbar_improved, background, false);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setNavigationOnClickListener(view -> onBackPressed());
		background.addView(toolbar, 0);

		fab_button.setOnClickListener(v -> {
			Object paletteColor = pallet_list.get(palette - 9).get("color");
			if (paletteColor instanceof String) {
				Intent intent = new Intent(getApplicationContext(), BlocksManagerCreatorActivity.class);
				intent.putExtra("mode", "add");
				intent.putExtra("color", (String) paletteColor);
				intent.putExtra("path", blocks_path);
				intent.putExtra("pallet", String.valueOf(palette));
				startActivity(intent);
			} else {
				SketchwareUtil.toastError("Invalid color of palette #" + (palette - 9));
			}
		});
	}

	public void openFileExplorerImport() {
		FilePickerOptions options = new FilePickerOptions();
		options.setExtensions(new String[]{"json"});
		options.setTitle("Select a JSON file");

		FilePickerCallback callback = new FilePickerCallback() {
			@Override
			public void onFileSelected(File file) {
				if (FileUtil.readFile(file.getAbsolutePath()).isEmpty()) {
					SketchwareUtil.toastError("The selected file is empty!");
				} else if (FileUtil.readFile(file.getAbsolutePath()).equals("[]")) {
					SketchwareUtil.toastError("The selected file is empty!");
				} else {
					try {
						ArrayList<HashMap<String, Object>> readMap = getGson().fromJson(FileUtil.readFile(file.getAbsolutePath()), Helper.TYPE_MAP_LIST);
						_importBlocks(readMap);
					} catch (JsonParseException e) {
						SketchwareUtil.toastError("Invalid JSON file");
					}
				}
			}
		};

		FilePickerDialogFragment dialog = new FilePickerDialogFragment(options, callback);

		dialog.show(getSupportFragmentManager(), "filePickerDialog");
	}

	@Override
	public void onStop() {
		super.onStop();
		listViewSavedState = block_list.onSaveInstanceState();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (listViewSavedState != null) {
			block_list.onRestoreInstanceState(listViewSavedState);
			_refreshLists();
		}
	}

	@Override
	public void onBackPressed() {
		if (mode.equals("editor")) {
			mode = "normal";
			Parcelable savedState = block_list.onSaveInstanceState();
			block_list.setAdapter(new Adapter(filtered_list));
			((BaseAdapter) block_list.getAdapter()).notifyDataSetChanged();
			block_list.onRestoreInstanceState(savedState);
			fabButtonVisibility(true);
			onCreateOptionsMenu(toolbar.getMenu());
		} else {
			finish();
		}
		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.clear();
		if (Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("position"))) != - 1) {
			if (mode.equals("normal")) {
				menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Swap").setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_swap_vertical)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Import");
				menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Export");
			} else {
				menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Swap").setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_mtrl_save)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
		String title = Objects.requireNonNull(menuItem.getTitle()).toString();
		switch (title) {
			case "Swap":
				if (mode.equals("normal")) {
					mode = "editor";
					fabButtonVisibility(false);
				} else {
					mode = "normal";
					fabButtonVisibility(true);
				}
				Parcelable savedInstanceState = block_list.onSaveInstanceState();
				block_list.setAdapter(new Adapter(filtered_list));
				((BaseAdapter) block_list.getAdapter()).notifyDataSetChanged();
				block_list.onRestoreInstanceState(savedInstanceState);
				onCreateOptionsMenu(toolbar.getMenu());
				break;

			case "Import":
				openFileExplorerImport();
				break;

			case "Export":
				Object paletteName = pallet_list.get(palette - 9).get("name");
				if (paletteName instanceof String) {
					String exportTo = new File(BLOCK_EXPORT_PATH, paletteName + ".json").getAbsolutePath();
					FileUtil.writeFile(exportTo, getGson().toJson(filtered_list));
					SketchwareUtil.toast("Successfully exported blocks to:\n" + exportTo, Toast.LENGTH_LONG);
				} else {
					SketchwareUtil.toastError("Invalid name of palette #" + (palette - 9));
				}
				break;

			default:
				return false;
		}
		return super.onOptionsItemSelected(menuItem);
	}

	private void _receive_intents() {
		palette = Integer.parseInt(Objects.requireNonNull(getIntent().getStringExtra("position")));
		pallet_path = getIntent().getStringExtra("dirP");
		blocks_path = getIntent().getStringExtra("dirB");
		_refreshLists();
		if (palette == - 1) {
			Objects.requireNonNull(getSupportActionBar()).setTitle("Recycle Bin");
			fab_button.setVisibility(View.GONE);
		} else {
			Object paletteName = pallet_list.get(palette - 9).get("name");

			if (paletteName instanceof String) {
				Objects.requireNonNull(getSupportActionBar()).setTitle("Manage Block");
				getSupportActionBar().setSubtitle((String) paletteName);
			}
		}
	}

	private void _refreshLists() {
		filtered_list.clear();
		reference_list.clear();

		// Ensure palette and block files exist before reading, creating them if necessary.
		if (! FileUtil.isExistFile(pallet_path)) {
			FileUtil.writeFile(pallet_path, "[]");
		}
		if (! FileUtil.isExistFile(blocks_path)) {
			FileUtil.writeFile(blocks_path, "[]");
		}

		String paletteFileContent = FileUtil.readFile(pallet_path);
		String blocksFileContent = FileUtil.readFile(blocks_path);

		parseLists:
		{
			try {
				pallet_list = getGson().fromJson(paletteFileContent, Helper.TYPE_MAP_LIST);

				if (pallet_list != null) {
					break parseLists;
				}
				// if file was empty or contained "null", fromJson returns null
				// fall-through to shared error handling
			} catch (JsonParseException e) {
				// fall-through to shared error handling
			}

			// Handle JSON parsing errors or null content
			SketchwareUtil.showFailedToParseJsonDialog(this, new File(pallet_path), "Custom Block Palettes", v -> _refreshLists());
			pallet_list = new ArrayList<>();
		}

		parseBlocks:
		{
			try {
				all_blocks_list = getGson().fromJson(blocksFileContent, Helper.TYPE_MAP_LIST);

				if (all_blocks_list != null) {
					break parseBlocks;
				}
				// if file was empty or contained "null", fromJson returns null
				// fall-through to shared error handling
			} catch (JsonParseException e) {
				SketchwareUtil.toastError("Invalid JSON file\n" + e.getMessage());
				// fall-through to shared error handling
			}

			// Handle JSON parsing errors or null content
			SketchwareUtil.showFailedToParseJsonDialog(this, new File(blocks_path), "Custom Blocks", v -> _refreshLists());
			all_blocks_list = new ArrayList<>();
		}

		for (int i = 0; i < all_blocks_list.size(); i++) {
			HashMap<String, Object> block = all_blocks_list.get(i);

			Object blockPalette = block.get("palette");
			if (blockPalette instanceof String) {
				try {
					if (Integer.parseInt((String) blockPalette) == palette) {
						reference_list.add(i);
						filtered_list.add(block);
					}
				} catch (NumberFormatException e) {
					SketchwareUtil.toastError("Invalid palette entry in block #" + (i + 1));
				}
			}
		}
		Parcelable onSaveInstanceState = block_list.onSaveInstanceState();
		block_list.setAdapter(new Adapter(filtered_list));
		((BaseAdapter) block_list.getAdapter()).notifyDataSetChanged();
		block_list.onRestoreInstanceState(onSaveInstanceState);
	}

	private void _swapitems(int sourcePosition, int targetPosition) {
		Collections.swap(all_blocks_list, sourcePosition, targetPosition);
		FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
		_refreshLists();
	}

	private void _showItemPopup(View view, int position) {
		if (palette == - 1) {
			PopupMenu popupMenu = new PopupMenu(this, view);
			Menu menu = popupMenu.getMenu();
			menu.add("Delete permanently");
			menu.add("Restore");
			popupMenu.setOnMenuItemClickListener(item -> {
				switch (Objects.requireNonNull(item.getTitle()).toString()) {
					case "Delete permanently":
						_deleteBlock(position);
						break;

					case "Restore":
						_changePallette(position);
						break;

					default:
						return false;
				}
				return true;
			});
			popupMenu.show();
			return;
		}
		PopupMenu popupMenu = new PopupMenu(this, view);
		Menu menu = popupMenu.getMenu();
		menu.add("Insert above");
		menu.add("Delete");
		menu.add("Duplicate");
		menu.add("Move to palette");
		popupMenu.setOnMenuItemClickListener(item -> {
			switch (Objects.requireNonNull(item.getTitle()).toString()) {
				case "Duplicate":
					_duplicateBlock(position);
					break;

				case "Insert above":
					Object paletteColor = pallet_list.get(palette - 9).get("color");
					// Assuming paletteList is a List<Map<String, Object>>
					/*Map<String, Object> colorMap = pallet_list.get(palette - 9);*/

					if (paletteColor != null && paletteColor instanceof String) {
						Intent intent = new Intent(getApplicationContext(), BlocksManagerCreatorActivity.class);
						intent.putExtra("mode", "insert");
						intent.putExtra("path", blocks_path);
						intent.putExtra("color", (String) paletteColor);
						intent.putExtra("pos", String.valueOf(position));
						startActivity(intent);
					} else {
						SketchwareUtil.toastError("Invalid color of palette #" + (palette - 9));
					}
					break;

				case "Move to palette":
					_changePallette(position);
					break;

				case "Delete":
					new MaterialAlertDialogBuilder(this)
							.setTitle("Delete block?")
							.setMessage("Are you sure you want to delete this block?")
							.setPositiveButton("Recycle bin", (dialog, which) -> _moveToRecycleBin(position))
							.setNegativeButton(R.string.common_word_cancel, null)
							.setNeutralButton("Delete permanently", (dialog, which) -> _deleteBlock(position))
							.show();
					break;

				default:
					return false;
			}
			return true;
		});
		popupMenu.show();
	}

	private void _duplicateBlock(int position) {
		HashMap<String, Object> block = new HashMap<>(all_blocks_list.get(position));
		Object blockName = block.get("name");

		if (blockName instanceof String) {
			if (((String) blockName).matches("(?s).*_copy[0-9][0-9]")) {
				block.put("name", ((String) blockName).replaceAll("_copy[0-9][0-9]", "_copy" + SketchwareUtil.getRandom(11, 99)));
			} else {
				block.put("name", blockName + "_copy" + SketchwareUtil.getRandom(11, 99));
			}
		}
		all_blocks_list.add(position + 1, block);
		FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
		_refreshLists();
	}

	private void _deleteBlock(int position) {
		all_blocks_list.remove(position);
		FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
		_refreshLists();
	}

	private void _moveToRecycleBin(int position) {
		all_blocks_list.get(position).put("palette", "-1");
		FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
		_refreshLists();
	}

	private void _changePallette(int position) {
		// 1. Verificar se a posição é válida
		if (position < 0 || position >= all_blocks_list.size()) {
			SketchwareUtil.toastError("Invalid block position: " + position);
			return;
		}

		// 2. Verificar se pallet_list não é nula e tem elementos
		if (pallet_list == null || pallet_list.isEmpty()) {
			SketchwareUtil.toastError("Palette list is empty or null");
			return;
		}

		// 3. Construir lista de nomes de paleta com segurança
		ArrayList<String> paletteNames = new ArrayList<>();
		for (int j = 0; j < pallet_list.size(); j++) {
			HashMap<String, Object> palette = pallet_list.get(j);
			if (palette == null) {
				SketchwareUtil.toastError("Palette #" + (j + 1) + " is null");
				continue;
			}

			Object nameObj = palette.get("name");
			if (nameObj instanceof String && ! ((String) nameObj).trim().isEmpty()) {
				paletteNames.add((String) nameObj);
			} else {
				String errorMsg = nameObj == null
						                  ? "Missing name in palette #" + (j + 1)
						                  : "Invalid name type in palette #" + (j + 1) + ": " + nameObj.getClass().getSimpleName();
				SketchwareUtil.toastError(errorMsg);
				paletteNames.add("Unnamed Palette #" + (j + 1)); // fallback
			}
		}

		if (paletteNames.isEmpty()) {
			SketchwareUtil.toastError("No valid palettes available");
			return;
		}

		// 4. Obter bloco atual com segurança
		HashMap<String, Object> currentBlock = all_blocks_list.get(position);
		if (currentBlock == null) {
			SketchwareUtil.toastError("Current block is null");
			return;
		}

		// 5. Determinar se é "Restore" ou "Move"
		Object paletteObj = currentBlock.get("palette");
		int currentPaletteIndex = - 1;
		if (paletteObj instanceof String) {
			try {
				currentPaletteIndex = Integer.parseInt((String) paletteObj);
			} catch (NumberFormatException e) {
				// ignorar, será tratado como -1
			}
		} else if (paletteObj instanceof Number) {
			currentPaletteIndex = ((Number) paletteObj).intValue();
		}

		final int palette = currentPaletteIndex; // valor final para uso em lambda

		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
				                                     .setNegativeButton(R.string.common_word_cancel, null);

		if (palette == - 1) {
			// Modo RESTORE
			AtomicInteger restoreToChoice = new AtomicInteger(- 1);
			builder.setTitle("Restore to")
					.setSingleChoiceItems(paletteNames.toArray(new String[0]), - 1,
							(dialog, which) -> restoreToChoice.set(which))
					.setPositiveButton("Restore", (dialog, which) -> {
						int choice = restoreToChoice.get();
						if (choice >= 0 && choice < paletteNames.size()) {
							currentBlock.put("palette", String.valueOf(choice + 9));
							swapAndSave(position);
						} else {
							SketchwareUtil.toastError("Invalid restore selection");
						}
					});
		} else {
			// Modo MOVE
			int initialSelection = palette - 9;
			if (initialSelection < 0 || initialSelection >= paletteNames.size()) {
				SketchwareUtil.toastError("Current palette index out of range: " + palette);
				return;
			}

			AtomicInteger moveToChoice = new AtomicInteger(initialSelection);
			builder.setTitle("Move to")
					.setSingleChoiceItems(paletteNames.toArray(new String[0]), initialSelection,
							(dialog, which) -> moveToChoice.set(which))
					.setPositiveButton("Move", (dialog, which) -> {
						int choice = moveToChoice.get();
						if (choice >= 0 && choice < paletteNames.size()) {
							currentBlock.put("palette", String.valueOf(choice + 9));
							swapAndSave(position);
						} else {
							SketchwareUtil.toastError("Invalid move selection");
						}
					});
		}

		try {
			builder.show();
		} catch (Exception e) {
			SketchwareUtil.toastError("Failed to show dialog: " + e.getMessage());
		}
	}

	// Método auxiliar para reutilizar swap + save
	private void swapAndSave(int position) {
		if (position < 0 || position >= all_blocks_list.size()) return;

		int lastIndex = all_blocks_list.size() - 1;
		if (position != lastIndex) {
			Collections.swap(all_blocks_list, position, lastIndex);
		}

		String json = getGson().toJson(all_blocks_list);
		if (json != null && FileUtil.isExistFile(blocks_path)) {
			FileUtil.writeFile(blocks_path, json);
			_refreshLists();
		} else {
			SketchwareUtil.toastError("Failed to save blocks data");
		}
	}

	private void _importBlocks(ArrayList<HashMap<String, Object>> blocks) {
		try {
			ArrayList<String> names = new ArrayList<>();
			ArrayList<Integer> toAdd = new ArrayList<>();
			for (int i = 0; i < blocks.size(); i++) {
				Object blockName = blocks.get(i).get("name");

				if (blockName instanceof String) {
					names.add((String) blockName);
				} else {
					SketchwareUtil.toastError("Invalid name entry of Custom Block #" + (i + 1) + " in Blocks to import");
				}
			}
			MaterialAlertDialogBuilder import_dialog = new MaterialAlertDialogBuilder(this);
			import_dialog.setTitle("Import blocks")
					.setMultiChoiceItems(names.toArray(new CharSequence[0]), null, (dialog, which, isChecked) -> {
						if (isChecked) {
							toAdd.add(which);
						} else {
							toAdd.remove((Integer) which);
						}
					})
					.setPositiveButton("Import", (dialog, which) -> {
						for (int i = 0; i < blocks.size(); i++) {
							if (toAdd.contains(i)) {
								HashMap<String, Object> map = blocks.get(i);
								map.put("palette", String.valueOf(palette));
								all_blocks_list.add(map);
							}
						}
						FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
						_refreshLists();
						SketchwareUtil.toast("Imported successfully");
					})
					.setNegativeButton("Reverse", (dialog, which) -> {
						for (int i = 0; i < blocks.size(); i++) {
							if (! toAdd.contains(i)) {
								HashMap<String, Object> map = blocks.get(i);
								map.put("palette", String.valueOf(palette));
								all_blocks_list.add(map);
							}
						}
						FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
						_refreshLists();
						SketchwareUtil.toast("Imported successfully");
					})
					.setNeutralButton("All", (dialog, which) -> {
						for (int i = 0; i < blocks.size(); i++) {
							HashMap<String, Object> map = blocks.get(i);
							map.put("palette", String.valueOf(palette));
							all_blocks_list.add(map);
						}
						FileUtil.writeFile(blocks_path, getGson().toJson(all_blocks_list));
						_refreshLists();
						SketchwareUtil.toast("Imported successfully");
					})
					.show();
		} catch (Exception e) {
			SketchwareUtil.toastError("An error occurred! [" + e.getMessage() + "]");
		}
	}

	private void fabButtonVisibility(boolean visible) {
		if (visible) {
			ObjectAnimator.ofFloat(fab_button, "translationX", fab_button.getTranslationX(), - 50.0f, 0.0f).setDuration(400L).start();
		} else {
			ObjectAnimator.ofFloat(fab_button, "translationX", fab_button.getTranslationX(), - 50.0f, 250.0f).setDuration(400L).start();
		}
	}

	private class Adapter extends BaseAdapter {

		private final ArrayList<HashMap<String, Object>> blocks;

		public Adapter(ArrayList<HashMap<String, Object>> data) {
			blocks = data;
		}

		@Override
		public int getCount() {
			return blocks.size();
		}

		@Override
		public HashMap<String, Object> getItem(int position) {
			return blocks.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.block_customview, parent, false);
			}

			HashMap<String, Object> block = blocks.get(position);

			LinearLayout background = convertView.findViewById(R.id.background);
			TextView name = convertView.findViewById(R.id.name);
			TextView spec = convertView.findViewById(R.id.spec);
			CardView upLayout = convertView.findViewById(R.id.up_layout);
			CardView downLayout = convertView.findViewById(R.id.down_layout);
			LinearLayout down = convertView.findViewById(R.id.down);
			LinearLayout up = convertView.findViewById(R.id.up);

			if (mode.equals("normal")) {
				downLayout.setVisibility(View.GONE);
				upLayout.setVisibility(View.GONE);
			} else {
				downLayout.setVisibility(position != blocks.size() - 1 ? View.VISIBLE : View.GONE);
				upLayout.setVisibility(position != 0 ? View.VISIBLE : View.GONE);
			}

			Object blockName = block.get("name");
			if (blockName instanceof String) {
				name.setText((String) blockName);
				spec.setHint("");
			} else {
				name.setText("");
				name.setHint("(Invalid block name entry)");
			}

			Object blockSpec = block.get("spec");
			if (blockSpec instanceof String) {
				spec.setText((String) blockSpec);
				spec.setHint("");
			} else {
				spec.setText("");
				spec.setHint("(Invalid block spec entry)");
			}

			Object blockType = block.get("type");
			if (blockType instanceof String) {
				switch ((String) blockType) {
					case " ":
					case "regular":
						spec.setBackgroundResource(R.drawable.block_ori);
						break;

					case "b":
						spec.setBackgroundResource(R.drawable.block_boolean);
						break;

					case "c":
					case "e":
						spec.setBackgroundResource(R.drawable.if_else);
						break;

					case "d":
						spec.setBackgroundResource(R.drawable.block_num);
						break;

					case "f":
						spec.setBackgroundResource(R.drawable.block_stop);
						break;

					default:
						spec.setBackgroundResource(R.drawable.block_string);
						break;
				}
			} else {
				spec.setBackgroundResource(R.drawable.block_string);
			}

			if (palette == - 1) {
				spec.getBackground().setColorFilter(new PorterDuffColorFilter(0xff9e9e9e, PorterDuff.Mode.MULTIPLY));
			} else {
				if (block.containsKey("color")) {
					Object blockColor = block.get("color");

					if (blockColor instanceof String) {
						int color = - 1;
						try {
							color = Color.parseColor((String) blockColor);
						} catch (IllegalArgumentException e) {
							SketchwareUtil.toastError("Invalid color entry in block #" + (position + 1));
						}

						if (color != - 1) {
							spec.getBackground().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
						}
					} else {
						SketchwareUtil.toastError("Invalid color entry in block #" + (position + 1));
					}
				} else {
					HashMap<String, Object> paletteObject = pallet_list.get(palette - 9);
					Object paletteColor = paletteObject.get("color");

					if (paletteColor instanceof String) {
						try {
							spec.getBackground().setColorFilter(new PorterDuffColorFilter(
									Color.parseColor((String) paletteColor),
									PorterDuff.Mode.MULTIPLY
							));
						} catch (IllegalArgumentException e) {
							SketchwareUtil.toastError("Invalid color in Custom Block palette #" + (palette - 8));
						}
					}
				}
			}
			up.setOnClickListener(v -> {
				if (position > 0) {
					_swapitems(reference_list.get(position), reference_list.get(position - 1));
				}
			});
			down.setOnClickListener(v -> {
				if (position < filtered_list.size() - 1) {
					_swapitems(reference_list.get(position), reference_list.get(position + 1));
				}
			});
			if (mode.equals("normal")) {
				background.setOnClickListener(v -> {
					if (palette == - 1) {
						_showItemPopup(background, reference_list.get(position));
					} else {
						Object paletteColor = pallet_list.get(palette - 9).get("color");

						if (paletteColor instanceof String) {
							Intent intent = new Intent(getApplicationContext(), BlocksManagerCreatorActivity.class);
							intent.putExtra("mode", "edit");
							intent.putExtra("color", (String) paletteColor);
							intent.putExtra("path", blocks_path);
							intent.putExtra("pos", String.valueOf(reference_list.get(position)));
							startActivity(intent);
						}
					}
				});
				background.setOnLongClickListener(v -> {
					_showItemPopup(background, reference_list.get(position));
					return true;
				});
			}
			return convertView;
		}
	}
}
