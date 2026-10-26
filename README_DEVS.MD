# Developer Documentation

## Source Code dev Map

These package maps cover contributor-maintained code under
`app/src/main/java/mod` and `app/src/main/java/dev/aldi/sayuti`.

### `app/src/main/java/mod`

| Package | Contents |
| ------- | -------- |
| `agus/jcoderz/beans` | View-related bean classes |
| `agus/jcoderz/dex` | DEX data structures and utilities |
| `agus/jcoderz/dx` | DEX/compiler tooling, including class-file parsing, translation, and optimization |
| `agus/jcoderz/editor` | Editor components, management, manifests, and widget views |
| `hey/studios` | Project, editor, compiler, build, library, and custom-block features |
| `hilal/saif` | Activities, Android manifest support, blocks, components, events, and libraries |
| `jbk/util` | Utility classes |
| `pranav` | Build/compiler tools, dependency resolution, diagnostics, editor/export support, and view binding |
| `tyron/backup` | Backup task support |
| `alucard`, `bobur`, `khaled`, `loddv`, `remaker` | Smaller contributor packages for view, conversion, logging, designer, and DEX class-path features |

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
