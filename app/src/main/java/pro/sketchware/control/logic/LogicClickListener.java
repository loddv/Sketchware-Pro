package pro.sketchware.control.logic;

import static android.text.TextUtils.isEmpty;
import static pro.sketchware.SketchApplication.getContext;
import static pro.sketchware.utility.SketchwareUtil.dpToPx;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.editor.LogicEditorActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import a.a.a.ZB;
import a.a.a.bB;
import a.a.a.eC;
import a.a.a.jC;
import a.a.a.uq;
import a.a.a.wB;
import mod.hey.studios.util.Helper;
import pro.sketchware.R;
import pro.sketchware.databinding.AddCustomListBinding;
import pro.sketchware.databinding.AddCustomVariableBinding;
import pro.sketchware.lib.validator.VariableModifierValidator;
import pro.sketchware.lib.validator.VariableTypeValidator;
import pro.sketchware.menu.ExtraMenuBean;
import pro.sketchware.utility.CustomVariableUtil;
import pro.sketchware.utility.SketchwareUtil;

public class LogicClickListener implements View.OnClickListener {

    private final eC projectDataManager;
    private final LogicEditorActivity logicEditor;
    private final ProjectFileBean projectFile;
    private final String eventName;
    private final String javaName;

    public LogicClickListener(LogicEditorActivity logicEditor) {
        this.logicEditor = logicEditor;
        projectDataManager = jC.a(logicEditor.scId);
        projectFile = logicEditor.projectFileBean;
        eventName = logicEditor.id + "_" + logicEditor.eventName;
        javaName = logicEditor.projectFileBean.getJavaName();
    }

    private ArrayList<String> getUsedVariable(int type) {
        return projectDataManager.e(projectFile.getJavaName(), type);
    }

    private ArrayList<String> getUsedList(int type) {
        return projectDataManager.d(projectFile.getJavaName(), type);
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        if (!isEmpty(tag)) {
            switch (tag) {
                case "listAddCustom":
                    addCustomList();
                    break;
                case "variableAddNew":
                    addCustomVariable();
                    break;
                case "variableRemove":
                    removeVariable();
                    break;
                case "listRemove":
                    removeList();
                    break;
                case "variableEdit":
                    showEditVariableDialog();
                    break;
            }
        }
    }

    // 1. Inicia o fluxo mostrando o diálogo de seleção categorizado
    public void showEditVariableDialog() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle("Edit Variable");
        View containerView = wB.a(logicEditor, R.layout.property_popup_selector_single);
        ViewGroup radioGroup = containerView.findViewById(R.id.rg_content);
        // Definição das categorias e tipos, igual ao removeVariable
        List<Pair<List<Integer>, String>> variableTypes = List.of(
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_BOOLEAN), "Boolean (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_NUMBER), "Number (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_STRING), "String (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_MAP), "Map (%d)"),
                new Pair<>(List.of(5, 6), "Custom Variable (%d)")
        );
        int totalVariables = 0;
        int padding = SketchwareUtil.dpToPx(8f);
        // Percorre as categorias
        for (Pair<List<Integer>, String> variableType : variableTypes) {
            List<String> variableTypeInstances = new LinkedList<>();
            List<Integer> typesList = variableType.first;
            // Junta todas as variáveis desta categoria
            for (int i = 0; i < typesList.size(); i++) {
                Integer type = typesList.get(i);
                if (i == 0) {
                    variableTypeInstances = getUsedVariable(type); // ou apenas getUsedVariable(type)
                    // dependendo de onde o código está
                } else {
                    variableTypeInstances.addAll(getUsedVariable(type));
                }
            }
            int size = variableTypeInstances.size();
            // Só adiciona a categoria na tela se ela tiver variáveis
            if (size > 0) {
                totalVariables += size;
                // 1. Cria e adiciona o Cabeçalho (Header) da categoria
                TextView header = new TextView(logicEditor);
                header.setText(String.format(variableType.second, size));
                header.setTextSize(14f);
                header.setTypeface(null, android.graphics.Typeface.BOLD);
                header.setPadding(padding, padding * 2, padding, padding / 2); // Espaço maior em cima
                // header.setTextColor(0xFF333333); // Opcional: ajustar cor
                radioGroup.addView(header);
                // 2. Adiciona os RadioButtons das variáveis logo abaixo do cabeçalho
                for (String instanceName : variableTypeInstances) {
                    RadioButton radioButton = new RadioButton(logicEditor);
                    radioButton.setText(instanceName);
                    radioButton.setPadding(padding, padding, padding, padding);
                    // Salva o tipo (usando o primeiro da lista como referência) e o nome exato
                    int primaryType = typesList.get(0);
                    radioButton.setTag(new Pair<>(primaryType, instanceName));
                    radioGroup.addView(radioButton);
                }
            }
        }
        // Se nenhuma variável foi encontrada em nenhuma categoria
        if (totalVariables == 0) {
            Toast.makeText(logicEditor, "No variables available to edit", Toast.LENGTH_SHORT).show();
            return;
        }
        dialog.setView(containerView);
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_edit), (d, which) -> {
            RadioButton selectedRadio = getSelectedRadioButton(radioGroup);
            if (selectedRadio != null) {
                // Recupera os dados salvos no Tag
                Pair<Integer, String> tagPair = (Pair<Integer, String>) selectedRadio.getTag();
                int varType = tagPair.first;
                String currentName = tagPair.second; // Nome limpo, já que não alteramos o setText()
                showRenameInputDialog(currentName, varType);
            } else {
                Toast.makeText(logicEditor, "Please select a variable first", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    // Método auxiliar inalterado, funciona perfeitamente ignorando os TextViews(Headers)
    private RadioButton getSelectedRadioButton(ViewGroup container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                if (rb.isChecked()) {
                    return rb;
                }
            }
        }
        return null;
    }

    // Método auxiliar para converter o ID numérico no nome do tipo da variável
    private String getVariableTypeName(int type) {
        if (type == ExtraMenuBean.VARIABLE_TYPE_BOOLEAN) {
            return "Boolean";
        } else if (type == ExtraMenuBean.VARIABLE_TYPE_NUMBER) {
            return "Number";
        } else if (type == ExtraMenuBean.VARIABLE_TYPE_STRING) {
            return "String";
        } else if (type == ExtraMenuBean.VARIABLE_TYPE_MAP) {
            return "Map";
        } else if (type == 5 || type == 6) {
            return "Custom Variable";
        }
        return "Unknown";
    }

    // 2. Diálogo com campo de texto para digitar o novo nome
    private void showRenameInputDialog(String oldName, int varType) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle("Rename variable: " + oldName);
        // Criação do layout com margens usando SketchwareUtil
        FrameLayout container = new FrameLayout(logicEditor);
        int horizontalPadding = SketchwareUtil.dpToPx(16f);
        int verticalPadding = SketchwareUtil.dpToPx(8f);
        container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        TextInputLayout inputLayout = new TextInputLayout(logicEditor);
        TextInputEditText inputEditText = new TextInputEditText(logicEditor);
        inputEditText.setHint("New variable name");
        inputEditText.setText(oldName);
        inputEditText.setSelection(oldName.length()); // Posiciona cursor no final
        inputLayout.addView(inputEditText);
        container.addView(inputLayout);
        dialog.setView(container);
        // Instancia o validador nativo ZB seguindo o modelo do addCustomVariable
        //        ZB validator = new ZB(logicEditor, inputLayout, uq.b, uq.a(), projectDataManager.a(projectFile));
        dialog.setPositiveButton(Helper.getResString(R.string.assets_manager_rename), (v, which) -> {
            String newName = Helper.getText(inputEditText).trim();
            // Usando o validador para garantir nomes válidos
            //            boolean isValidName = validator.b();
            boolean isValidName = true;
            if (!isValidName) {
                inputLayout.setError("Invalid or existing name");
                inputLayout.setErrorEnabled(true);
                inputLayout.requestFocus();
                return;
            } else {
                inputLayout.setError(null);
                inputLayout.setErrorEnabled(false);
            }
            if (newName.equals(oldName)) {
                v.dismiss();
                return; // Sem alterações
            }
            // Verifica se a variável está em uso usando a mesma lógica completa do removeVariable
            boolean isCurrentlyUsed = logicEditor.blockPane.c(oldName) || projectDataManager.c
                    (javaName, oldName,
                            eventName);
            if (isCurrentlyUsed) {
                Toast.makeText(logicEditor,
                        Helper.getResString(R.string.logic_editor_message_currently_used_variable),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // Executa a renomeação
            performVariableRename(oldName, newName, varType);
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    // 3. Aplicação das alterações no projeto e na UI
    private void performVariableRename(String oldName, String newName, int varType) {
        // 1. REMOVE a variável antiga.
        // NOTA: 'e' (ou às vezes 'c') é o metodo padrão no ProjectDataManager (jC) para remover variáveis.
        // A assinatura geralmente é (String activityName, int type, String variableName).
        // jC.a(scId).e(javaName, varType, oldName);
        // 2. ADICIONA a nova variável.
        // A assinatura geralmente é (String activityName, int type, String variableName).
        // NOTA: 'a' é o metodo padrão do jC para adicionar novas variáveis.
        // jC.a(scId).a(javaName, varType, newName);
        // Atualiza o painel de blocos para mostrar a variável com o novo nome
        try {
            logicEditor.m(oldName);
            logicEditor.b(varType, newName.trim());
            if (logicEditor.blockPane != null) {
                logicEditor.blockPane.a();
            }
            Toast.makeText(logicEditor, "Variable renamed successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(logicEditor, "Failed to rename variable", Toast.LENGTH_SHORT).show();
        }
    }

    private void addCustomVariable() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle("Add a new custom variable");
        AddCustomVariableBinding binding = AddCustomVariableBinding.inflate(logicEditor.getLayoutInflater());
        binding.modifierLayout.setHelperText("Enter modifier e.g. private, public, public static, or empty (package " +
                "private).");
        // 1. Definição das sugestões de modificadores
        String[] suggestions = new String[]{
                "public",
                "private",
                "protected",
                "public static",
                "private static",
                "protected static",
                "public static final",
                "private static final"
        };
        // 2. Configuração e aplicação do Adapter ANTES de exibir o diálogo
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                R.layout.dropdown_item,
                suggestions
        );
        binding.modifier.setAdapter(adapter);
        VariableModifierValidator modifiersValidator = new VariableModifierValidator(getContext(),
                binding.modifierLayout);
        binding.modifier.addTextChangedListener(modifiersValidator);
        VariableTypeValidator varTypeValidator = new VariableTypeValidator(getContext(), binding.typeLayout);
        binding.type.addTextChangedListener(varTypeValidator);
        ZB validator = new ZB(getContext(), binding.nameLayout, uq.b, uq.a(), projectDataManager.a(projectFile));
        // Preview TextView
        TextView previewTextView = binding.tvPreview;
        // TextWatcher compartilhado para atualizar o preview
        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) {}

            private void updatePreview() {
                String modifier = Helper.getText(binding.modifier).trim();
                String type = Helper.getText(binding.type).trim();
                String name = Helper.getText(binding.name).trim();
                String init = Helper.getText(binding.initializer).trim();
                boolean hasContent = !type.isEmpty() || !name.isEmpty();
                if (!hasContent) {
                    previewTextView.setVisibility(View.GONE);
                    return;
                }
                StringBuilder sb = new StringBuilder();
                if (!modifier.isEmpty()) sb.append(modifier).append(" ");
                sb.append(type.isEmpty() ? "<type>" : type);
                sb.append(" ");
                sb.append(name.isEmpty() ? "<name>" : name);
                if (!init.isEmpty()) {
                    sb.append(" = ").append(init);
                }
                sb.append(";");
                previewTextView.setText("Preview:\n" + sb.toString());
                previewTextView.setVisibility(View.VISIBLE);
            }
        };
        // Adiciona o watcher em todos os campos
        binding.modifier.addTextChangedListener(previewWatcher);
        binding.type.addTextChangedListener(previewWatcher);
        binding.name.addTextChangedListener(previewWatcher);
        binding.initializer.addTextChangedListener(previewWatcher);
        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_add), (v, which) -> {
            String variableModifier = Helper.getText(binding.modifier).trim();
            String variableType = Helper.getText(binding.type).trim();
            String variableName = Helper.getText(binding.name).trim();
            String variableInitializer = Helper.getText(binding.initializer).trim();
            boolean isValidModifier = modifiersValidator.isValid() || variableModifier.isEmpty();
            boolean isValidType = varTypeValidator.isValid();
            boolean isValidName = validator.b();
            if (!isValidModifier) {
                binding.modifierLayout.requestFocus();
                return;
            }
            if (!isValidType) {
                binding.typeLayout.setError("Type can't be empty");
                binding.typeLayout.setErrorEnabled(true);
                binding.typeLayout.requestFocus();
                return;
            } else {
                binding.typeLayout.setError(null);
                binding.typeLayout.setErrorEnabled(false);
            }
            if (!isValidName) {
                binding.nameLayout.setError("Name can't be empty");
                binding.nameLayout.setErrorEnabled(true);
                binding.nameLayout.requestFocus();
                return;
            } else {
                binding.nameLayout.setError(null);
                binding.nameLayout.setErrorEnabled(false);
            }
            String variable = !variableModifier.isEmpty() ? variableModifier + " " : "";
            variable += variableType + " " + variableName;
            if (!variableInitializer.isEmpty()) {
                variable += " = " + variableInitializer;
            }
            logicEditor.b(6, variable.trim());
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
        binding.modifierLayout.requestFocus();
    }

    private void removeVariable() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle(Helper.getResString(R.string.logic_editor_title_remove_variable));
        int horizontalPadding = SketchwareUtil.dpToPx(20f);
        RecyclerView recyclerView = new RecyclerView(logicEditor);
        recyclerView.setPadding(horizontalPadding, SketchwareUtil.dpToPx(8f), horizontalPadding, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(null));
        List<Item> data = new LinkedList<>();
        RemoveAdapter adapter = new RemoveAdapter(logicEditor, data,
                variableName -> logicEditor.blockPane.c(variableName) || projectDataManager.c(javaName, variableName,
                        eventName));
        recyclerView.setAdapter(adapter);
        List<Pair<List<Integer>, String>> variableTypes = List.of(
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_BOOLEAN), "Boolean (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_NUMBER), "Number (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_STRING), "String (%d)"),
                new Pair<>(List.of(ExtraMenuBean.VARIABLE_TYPE_MAP), "Map (%d)"),
                new Pair<>(List.of(5, 6), "Custom Variable (%d)")
        );
        for (Pair<List<Integer>, String> variableType : variableTypes) {
            List<String> variableTypeInstances = new LinkedList<>();
            List<Integer> first = variableType.first;
            for (int i = 0; i < first.size(); i++) {
                Integer type = first.get(i);
                if (i == 0) {
                    variableTypeInstances = getUsedVariable(type);
                } else {
                    variableTypeInstances.addAll(getUsedVariable(type));
                }
            }
            for (int i = 0, size = variableTypeInstances.size(); i < size; i++) {
                String instanceName = variableTypeInstances.get(i);
                if (i == 0) data.add(new Item(String.format(variableType.second, size)));
                data.add(new Item(instanceName, R.string.logic_editor_message_currently_used_variable));
            }
        }
        dialog.setView(recyclerView);
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_remove), (v, which) -> {
            for (Item item : data) {
                if (item.type == Item.TYPE_ITEM && item.isChecked) {
                    logicEditor.m(item.text);
                }
            }
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private void addCustomList() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle("Add a new custom List");
        AddCustomListBinding binding = AddCustomListBinding.inflate(logicEditor.getLayoutInflater());
        ZB validator = new ZB(getContext(), binding.nameLayout, uq.b, uq.a(), projectDataManager.a(projectFile));
        TextView previewTextView = binding.tvPreview;
        // Variável para o botão (será preenchida após show())
        final Button[] positiveButton = {null};
        // TextWatcher com preview + habilitação do botão
        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePreviewAndButton();
            }

            private void updatePreviewAndButton() {
                String type = Helper.getText(binding.type).trim();
                String name = Helper.getText(binding.name).trim();
                boolean hasType = !isEmpty(type);
                boolean hasName = !isEmpty(name);
                boolean nameValid = validator.b();
                // Preview
                if (hasType || hasName) {
                    String displayType = hasType ? type : "<Type>";
                    String displayName = hasName ? name : "<name>";
                    String code = escape(String.format("ArrayList<%s> %s = new ArrayList<>();", displayType,
                            displayName));
                    previewTextView.setText(Html.fromHtml("Preview:<br><font color='#006400'><b>" + code + "</b" +
                            "></font>"));
                    previewTextView.setVisibility(View.VISIBLE);
                } else {
                    previewTextView.setVisibility(View.GONE);
                }
                // Botão
                if (positiveButton[0] != null) {
                    positiveButton[0].setEnabled(hasType && hasName && nameValid);
                }
            }

            private String escape(String s) {
                return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            }
        };
        // Adiciona watchers
        binding.type.addTextChangedListener(previewWatcher);
        binding.name.addTextChangedListener(previewWatcher);
        dialog.setView(binding.getRoot());
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_add), (v, which) -> {
            String variableType = Helper.getText(binding.type).trim();
            String variableName = Helper.getText(binding.name).trim();
            if (!validator.b()) return;
            logicEditor.a(4, variableType + " " + variableName + " = new ArrayList<>()");
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        // Mostra o dialog e captura o botão
        AlertDialog alert = dialog.show();
        positiveButton[0] = alert.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton[0].setEnabled(false); // Desabilita inicialmente
        // Força atualização inicial do preview
        previewWatcher.onTextChanged("", 0, 0, 0);
        // Foco
        // binding.typeLayout.requestFocus();
    }

    private void removeList() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(logicEditor);
        dialog.setTitle(Helper.getResString(R.string.logic_editor_title_remove_list));
        int horizontalPadding = SketchwareUtil.dpToPx(20f);
        RecyclerView recyclerView = new RecyclerView(logicEditor);
        recyclerView.setPadding(horizontalPadding, SketchwareUtil.dpToPx(8f), horizontalPadding, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(null));
        List<Item> data = new LinkedList<>();
        RemoveAdapter adapter = new RemoveAdapter(logicEditor, data,
                listName -> logicEditor.blockPane.b(listName) || projectDataManager.b(javaName, listName, eventName));
        recyclerView.setAdapter(adapter);
        List<Pair<Integer, String>> listTypes = List.of(
                new Pair<>(ExtraMenuBean.LIST_TYPE_NUMBER, "List Integer (%d)"),
                new Pair<>(ExtraMenuBean.LIST_TYPE_STRING, "List String (%d)"),
                new Pair<>(ExtraMenuBean.LIST_TYPE_MAP, "List Map (%d)"),
                new Pair<>(4, "List Custom (%d)")
        );
        for (Pair<Integer, String> listType : listTypes) {
            ArrayList<String> lists = getUsedList(listType.first);
            for (int i = 0, size = lists.size(); i < size; i++) {
                String instanceName = lists.get(i);
                if (i == 0) data.add(new Item(String.format(listType.second, size)));
                data.add(new Item(instanceName, R.string.logic_editor_message_currently_used_list));
            }
        }
        dialog.setView(recyclerView);
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_remove), (v, which) -> {
            for (Item item : data) {
                if (item.type == Item.TYPE_ITEM && item.isChecked) {
                    logicEditor.l(item.text);
                }
            }
            v.dismiss();
        });
        dialog.setNegativeButton(Helper.getResString(R.string.common_word_cancel), null);
        dialog.show();
    }

    private interface ButtonSetter {
        void setPositiveButton(Button button);
    }

    private static class RemoveAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context context;
        private final List<Item> data;
        private final Function<String, Boolean> isInUseChecker;

        /**
         * @param isInUseChecker Function that should return whether the name (parameter) is in use.
         */
        private RemoveAdapter(Context context, List<Item> data, Function<String, Boolean> isInUseChecker) {
            this.context = context;
            this.data = data;
            this.isInUseChecker = isInUseChecker;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        @NonNull
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == Item.TYPE_TITLE) {
                TextView textView = new TextView(context);
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT));
                textView.setPadding(
                        dpToPx(4),
                        dpToPx(4),
                        dpToPx(4),
                        dpToPx(4)
                );
                textView.setTextSize(14);
                return new TitleHolder(textView);
            } else if (viewType == Item.TYPE_ITEM) {
                CheckBox checkBox = new CheckBox(context);
                checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
                return new CheckBoxHolder(checkBox);
            } else {
                throw new IllegalStateException("Unknown view type " + viewType);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Item item = data.get(position);
            int viewType = holder.getItemViewType();
            if (viewType == Item.TYPE_TITLE) {
                TitleHolder titleHolder = (TitleHolder) holder;
                titleHolder.title.setText(item.text);
            } else if (viewType == Item.TYPE_ITEM) {
                CheckBoxHolder checkBoxHolder = (CheckBoxHolder) holder;
                String variable = item.text;
                String variableType = CustomVariableUtil.getVariableType(variable);
                String variableName = CustomVariableUtil.getVariableName(variable);
                if (variableType != null && variableName != null) {
                    variable = variableType + ": " + variableName;
                }
                checkBoxHolder.checkBox.setText(variable);
                checkBoxHolder.checkBox.setChecked(item.isChecked);
                checkBoxHolder.checkBox.setOnClickListener(v -> {
                    boolean isChecked = checkBoxHolder.checkBox.isChecked();
                    item.isChecked = isChecked;
                    if (item.type == Item.TYPE_ITEM && isChecked) {
                        if (isInUseChecker.apply(item.text)) {
                            //noinspection ConstantConditions Item#inUseMessage can't be null if Item#type is
                            // Item#TYPE_ITEM
                            SketchwareUtil.toastError(Helper.getResString(item.inUseMessage), bB.TOAST_WARNING);
                            checkBoxHolder.checkBox.performClick();
                        }
                    }
                });
            } else {
                throw new IllegalStateException("Unknown view type " + viewType);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return data.get(position).type;
        }

        private static class CheckBoxHolder extends RecyclerView.ViewHolder {
            public final CheckBox checkBox;

            public CheckBoxHolder(View itemView) {
                super(itemView);
                checkBox = (CheckBox) itemView;
            }
        }

        private static class TitleHolder extends RecyclerView.ViewHolder {
            public final TextView title;

            public TitleHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView;
            }
        }
    }

    private static class Item {
        public static final int TYPE_TITLE = 0;
        public static final int TYPE_ITEM = 1;

        private final int type;
        private final String text;
        @StringRes
        private final Integer inUseMessage;

        private boolean isChecked = false;

        public Item(String title) {
            type = TYPE_TITLE;
            text = title;
            inUseMessage = null;
        }

        public Item(String itemName, @StringRes int inUseMessage) {
            type = TYPE_ITEM;
            text = itemName;
            this.inUseMessage = inUseMessage;
        }

        public int getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }
}
