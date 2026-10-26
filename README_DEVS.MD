# Developer Documentation

## Source Code dev Map

These package maps cover contributor-maintained code under
`app/src/main/java/mod` and `app/src/main/java/dev/aldi/sayuti`.

### `app/src/main/java/mod`

| Package | Contents |
| ------- | -------- |
| `agus/jcoderz/beans` | Bean/data classes used by views |
| `agus/jcoderz/dex` | DEX file model, identifiers, encoded values, and binary read/write utilities |
| `agus/jcoderz/dx/cf` | Class-file attributes, bytecode, constants, direct parsing, and interfaces |
| `agus/jcoderz/dx/command` | `dx` command-line entry points and tools for annotation processing, dexing, dumps, usage searches, and grep |
| `agus/jcoderz/dx/dex` | Translation of class-file structures and code into DEX; code forms and DEX file assembly |
| `agus/jcoderz/dx/io` | DEX instruction formats, decoding, and low-level input/output |
| `agus/jcoderz/dx/merge` | DEX merge operations |
| `agus/jcoderz/dx/rop` | Register-oriented intermediate representation: annotations, code, constants, and types |
| `agus/jcoderz/dx/ssa` | SSA conversion and optimization, including back-end passes |
| `agus/jcoderz/dx/util` | Shared compiler and DEX utility classes |
| `agus/jcoderz/editor/event` | Editor event integration |
| `agus/jcoderz/editor/library` | Editor library integration |
| `agus/jcoderz/editor/manage/block` | Block creation and palette management |
| `agus/jcoderz/editor/manage/library` | Local library management |
| `agus/jcoderz/editor/manage/permission` | Permission management |
| `agus/jcoderz/editor/manage/resource` | Resource management |
| `agus/jcoderz/editor/manifest` | Android manifest editing and support |
| `agus/jcoderz/editor/view` | Editor item views and palette views |
| `agus/jcoderz/handle/component` | Component-related handlers |
| `agus/jcoderz/multidex` | Multi-DEX support |
| `alucard/tn/apksigner` | APK signing |
| `bobur` | XML-to-SVG conversion |
| `hey/studios/activity/managers` | Activities for managing project assets, Java files, and native libraries |
| `hey/studios/build` | Build settings |
| `hey/studios/code` | Source-code editor integration |
| `hey/studios/compiler/kotlin` | Kotlin compiler bridge and utilities |
| `hey/studios/editor` | Extra blocks, block loading, and editor view support |
| `hey/studios/lib` | Library checks and code-editor components, themes, and color schemes |
| `hey/studios/moreblock` | More-block validation, return-block management, and importing |
| `hey/studios/project` | Project settings/tracking, backup and restore, custom blocks, ProGuard, and string obfuscation |
| `hey/studios/util` | Shared project, compilation-log, and system-log helpers |
| `hilal/saif/activities` | Android manifest injection and project/tool activities |
| `hilal/saif/android_manifest` | Android manifest injection |
| `hilal/saif/asd` | ASD dialogs and code-editor support |
| `hilal/saif/blocks` | Block handlers and command-block definitions |
| `hilal/saif/components` | Component handlers and extra component code |
| `hilal/saif/events` | Event and logic handlers |
| `hilal/saif/lib` | Supporting library classes |
| `jbk/build` | Build progress and built-in library support |
| `jbk/build/compiler` | App bundle, DEX, and Android resource compilers |
| `jbk/code` | Code-editor languages and color schemes |
| `jbk/diagnostic` | Compilation diagnostics and missing-file errors |
| `jbk/editor/manage` | More-block import and built-in library settings |
| `jbk/export` | Export and keystore credential UI |
| `jbk/util` | Build, resource-ID, audio, logging, block, and window-inset utilities |
| `khaled/logcat` | Logcat reader activity |
| `loddv/dev/designer` | Designer grid options |
| `loddv/dev/manager` | Repository and string-management UI/models |
| `loddv/dev/tools/offlineTranslator` | Offline translation client, project translation, batch translation, and translation service types |
| `pranav/build` | JAR task and R8 compiler |
| `pranav/dependency/resolver` | Dependency resolution |
| `pranav/viewbinding` | View-binding code generation |
| `remaker/view` | Custom-attribute editor view |
| `tyron/backup` | Backup callback/task support |

### `app/src/main/java/dev/aldi/sayuti`

| Package | Contents |
| ------- | -------- |
| `block` | Extra block and palette-block definitions |
| `editor/injection` | AppCompat and Android manifest injection |
| `editor/manage` | Local-library models, utilities, comparison, download, and management UI |
| `editor/view/item` | Editor-side views for additional component types |
| `editor/view/palette` | Palette icons for additional component types |

When changing a component, check both its palette representation and editor-side
view where applicable. The `mod` and `dev/aldi/sayuti` packages are separate
locations; follow the package and directory structure already used by the feature.
