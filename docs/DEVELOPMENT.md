# Manual de desenvolvimento do Sketchware Pro

Este manual descreve o código e a configuração presentes nesta branch. Como o projeto mantém partes históricas e várias gerações de código, os nomes e caminhos abaixo correspondem à estrutura atual do repositório, não a uma arquitetura idealizada ou a outra branch.

## 1. Visão geral

Sketchware Pro é um aplicativo Android para criar projetos Android por meio de um editor visual. O próprio aplicativo também gera código, compila recursos e fontes, empacota e assina APKs dos projetos criados.

Configuração atual do aplicativo, em `app/build.gradle`:

| Propriedade | Valor |
| --- | --- |
| Módulo Gradle | `:app` (único módulo incluído em `settings.gradle`) |
| `applicationId` / namespace | `pro.sketchware` |
| `versionName` / `versionCode` | `v7.1.0` / `150` |
| `compileSdk` / `minSdk` / `targetSdk` | `36` / `26` / `28` |
| Compatibilidade Java | Java 17 |
| Gradle Wrapper | Gradle 9.4.1 |
| Android Gradle Plugin | 9.5.0-alpha06 |
| Kotlin Gradle Plugin | 2.3.21 |

O `targetSdk` 28 é uma escolha explícita do aplicativo, que não é distribuído pela Google Play. Consulte o `AndroidManifest.xml` e as configurações de permissões antes de alterar o comportamento de armazenamento ou instalação.

## 2. Ambiente de desenvolvimento

### Requisitos

- JDK e Android SDK configurados para o Gradle/Android Studio.
- Plataforma Android SDK 36; deixe o SDK Manager instalar componentes adicionais solicitados pelo Gradle.
- A configuração `gradle/gradle-daemon-jvm.properties` fixa o daemon Gradle no Java 21. Alguns workflows de CI ainda configuram Java 17, portanto há uma diferença entre configurações locais e CI: verifique a saída de `./gradlew --version` e a resolução de toolchains se houver falha de inicialização.
- Git e acesso à rede para baixar o Gradle e as dependências Maven.

### Clonar e compilar

```bash
git clone <url-do-fork>
cd Sketchware-Pro
./gradlew --version
./gradlew assembleDebug
```

Também é possível abrir a raiz do repositório no Android Studio e deixar o IDE sincronizar o Gradle. O APK debug é gerado em `app/build/outputs/apk/debug/`.

Para compilar a variante release:

```bash
./gradlew assembleRelease
```

### Serviços e chaves

- `SKETCHUB_API_KEY`, quando definida no ambiente, é incorporada ao `BuildConfig` do aplicativo. Sem ela, os recursos que dependem do SketchHub podem não funcionar; ela não é necessária para compilar a maior parte do aplicativo.
- O plugin Google Services é aplicado no módulo `app`. A task `createMockGoogleServices`, em `gradle/custom-tasks.gradle`, é executada antes de `preBuild`: com `GOOGLE_SERVICES_JSON` definido, grava o conteúdo em `app/google-services.json`; sem ele, cria uma configuração mock para debug em `app/src/debug/google-services.json` quando necessário.
- Não versione credenciais ou arquivos reais de serviços. Os workflows de CI podem injetar configurações privadas durante o build.
- A configuração de assinatura debug usa `testkey.keystore` na raiz. A variante release também referencia a configuração debug e `minifyEnabled` está desativado. Isso é configuração de desenvolvimento, não uma chave adequada para publicar uma distribuição própria.

## 3. Mapa do código

O código Android fica em `app/src/main/`. A organização é histórica e **não** foi totalmente migrada para `pro.sketchware`:

| Caminho | Responsabilidade principal |
| --- | --- |
| `pro/sketchware/` | Aplicação, utilitários e funcionalidades adicionadas ou reorganizadas mais recentemente |
| `com/besome/sketch/` | Activities, beans e funcionalidades herdadas do Sketchware |
| `mod/` | Extensões e subsistemas trazidos pelo projeto e por colaboradores |
| `a/a/a/` | Classes internas e componentes centrais legados, alguns com nomes curtos/obfuscados |
| `app/src/main/res/` | Layouts, strings, temas, ícones e demais recursos Android |
| `app/src/main/assets/` | Bibliotecas e arquivos distribuídos como assets do aplicativo |
| `gradle/` | Catálogo de versões, wrapper e tasks Gradle auxiliares |
| `.github/workflows/` | Build e automações de CI |

Pontos de entrada e arquivos úteis:

| Área | Arquivos |
| --- | --- |
| Inicialização do processo | `pro/sketchware/SketchApplication.java` |
| Tela inicial | `pro/sketchware/activities/main/activities/MainActivity.java` |
| Designer visual | `com/besome/sketch/design/DesignActivity.java` |
| Editor de lógica | `com/besome/sketch/editor/LogicEditorActivity.java` |
| Modelos de projeto/blocos | `com/besome/sketch/beans/ProjectBean.java`, `ProjectFileBean.java`, `ViewBean.java`, `ComponentBean.java`, `EventBean.java`, `BlockBean.java` |
| Geração de código | `a/a/a/Jx.java`, `a/a/a/Lx.java`, `a/a/a/Ox.java`, `a/a/a/Fx.java` |
| Build dos projetos criados no app | `a/a/a/ProjectBuilder.java` |
| Dependências Maven de projetos | `mod/pranav/dependency/resolver/DependencyResolver.kt` |
| Caminhos de arquivos do projeto | `pro/sketchware/utility/FilePathUtil.java`, `a/a/a/yq.java` |
| Erros de compilação no app | `mod/jbk/diagnostic/CompileErrorSaver.java` |

`SketchApplication` guarda um contexto global e aplica o tema inicial. Também instala um handler global de exceções não capturadas que abre `CollectErrorActivity` antes de encerrar o processo.

## 4. Fluxo de dados e geração

### Edição e persistência

`MainActivity` abre o projeto; `DesignActivity` edita a tela e seus componentes; `LogicEditorActivity` edita os blocos associados a eventos. Os objetos de `com.besome.sketch.beans` transportam os dados entre editor, armazenamento e geradores.

`BlockBean` representa um bloco com opcode, tipo/retorno, especificação visual, parâmetros e referências a blocos seguintes ou pilhas aninhadas (`nextBlock`, `subStack1`, `subStack2`). Ao alterar um bean ou a serialização do projeto, considere os dados já salvos pelos usuários e mantenha compatibilidade com projetos existentes.

Os projetos e arquivos auxiliares usam armazenamento externo legado sob `.sketchware/`, incluindo caminhos como `.sketchware/data/<sc_id>/`. `FilePathUtil` centraliza vários desses caminhos. Para arquivos de projeto, prefira os utilitários existentes a introduzir caminhos absolutos ou strings duplicadas.

### Blocos e código gerado

O código de lógica é dividido entre definições de blocos e a implementação de geração:

- `pro/sketchware/blocks/ExtraBlocks.java` adiciona definições em formato de template.
- `mod/hilal/saif/blocks/BlocksHandler.java` reúne essas definições e outros blocos internos.
- `mod/hey/studios/editor/manage/block/v2/BlockLoader.java` carrega definições customizadas e oferece `getBlockInfo(...)`.
- `a/a/a/Fx.java` contém a geração de código para muitos opcodes internos; sua implementação usa dispatch e templates Java.
- `mod/hilal/saif/blocks/CommandBlock.java` trata comandos que inserem/modificam código gerado.
- Blocos definidos pelo usuário são lidos de `.sketchware/resources/block/My Block/block.json`; definições específicas de projeto também são consultadas em `.sketchware/data/<sc_id>/custom_blocks`.

Para adicionar um bloco interno simples, inclua uma definição em `ExtraBlocks.extraBlocks(...)` usando o helper `addBlock(name, type, typeName, code, color, spec)`. O `code` é um template Java com marcadores de parâmetro (`%s`, `%1$s` etc.); `spec` define a aparência/slots do bloco no editor. O opcode precisa ser único e consistente com a lógica que o interpreta. Os tipos usados nas definições existentes incluem `" "` para instruções, `"b"` para booleanos, `"d"` para números, `"s"` para strings, `"v"` para views e `"c"` para blocos compostos.

Se o bloco precisar de comportamento especial, atualize também o dispatcher apropriado em `Fx.java` (ou o subsistema específico, como os command blocks). Não basta declarar o bloco na paleta: a definição visual e a geração precisam concordar quanto a opcode, quantidade/ordem de parâmetros e tipo de retorno. Verifique também imports, dependências e o código produzido para os projetos-alvo.

### Componentes, eventos e layouts

`a/a/a/Jx.java`, `Lx.java` e `Ox.java` participam da geração de código de Activities, componentes/listeners e layouts XML, respectivamente. A integração de componentes também envolve `mod/hilal/saif/components/ComponentsHandler.java` e os beans em `com/besome/sketch/beans/`.

Ao adicionar um componente ou evento, rastreie o fluxo completo: definição no editor, persistência do bean, geração de inicialização/listeners, imports e recursos necessários. Faça a mudança nas classes existentes do subsistema, em vez de criar um segundo caminho de geração desconectado.

## 5. Build dos aplicativos criados no Sketchware

O build executado **dentro do app** é diferente de `./gradlew assembleDebug`, que compila o próprio Sketchware Pro. `DesignActivity` e `ExportProjectActivity` conduzem o build do projeto do usuário por meio de `a/a/a/ProjectBuilder.java`, reportando etapas por `mod/jbk/build/BuildProgressReceiver.java`.

O fluxo normal inclui:

1. Preparar diretórios temporários e gerar fontes/recursos do projeto.
2. Extrair bibliotecas internas necessárias ao build.
3. Compilar e vincular recursos Android com `mod/jbk/build/compiler/resource/ResourceCompiler.java` e AAPT2.
4. Gerar View Binding quando habilitado e compilar Kotlin quando há código Kotlin.
5. Compilar Java, gerar DEX (Dx ou D8, conforme configuração) e, quando ativado, executar etapas opcionais de StringFog ou R8/ProGuard.
6. Empacotar, alinhar e assinar o APK; a tela de design pode então solicitar a instalação.

`ProjectBuilder` contém as operações centrais, entre elas `compileResources()`, `generateViewBinding()`, `compileJavaCode()`, `createDexFilesFromClasses()`, `buildApk()` e `signDebugApk()`. O progresso e a ordem exata das etapas são coordenados pelas Activities; atualize ambos se inserir uma nova fase.

As opções por projeto estão em `mod/hey/studios/build/BuildSettings.java` e `mod/hey/studios/project/ProjectSettings.java`. A compilação de dependências externas fica em `DependencyResolver.kt`: aceita artefatos Maven JAR/AAR, baixa e transforma dependências para uso no build interno. Considere falhas de rede, empacotamento inválido e callbacks de progresso ao alterar esse fluxo.

## 6. Como estender o projeto

### Nova tela ou funcionalidade Android

1. Siga a estrutura e os componentes usados pela funcionalidade mais próxima.
2. Para código novo que não precise ficar em um pacote histórico, prefira `pro.sketchware.*`, conforme a orientação do `README.md`.
3. Registre Activities, providers, permissões e intent filters em `app/src/main/AndroidManifest.xml` quando necessário.
4. Coloque strings traduzíveis em `res/values/strings.xml` e mantenha os recursos Android consistentes entre variantes/locales existentes.

### Novo opcode/bloco

1. Defina metadata e template em `ExtraBlocks.extraBlocks(...)`, ou no local equivalente em `BlocksHandler` se fizer parte da lista de blocos internos.
2. Implemente ou conecte o opcode em `a/a/a/Fx.java` quando o template não for suficiente.
3. Confirme parâmetros, tipo de retorno, imports e nomes de classes esperados no código produzido.
4. Teste o bloco no Logic Editor e compile um projeto mínimo que o utilize, incluindo os caminhos de controle e valores de entrada relevantes.

### Nova biblioteca de projeto

Inspecione `DependencyResolver.kt`, `mod/hey/studios/build/BuildSettings.java` e os gerenciadores em `mod/agus/jcoderz/editor/manage/library/` antes de alterar downloads, classpath, recursos ou DEX. Mantenha os diretórios compatíveis com `FilePathUtil` e reporte progresso/erros pelas interfaces já usadas pela UI.

## 7. Convenções e compatibilidade

- O `README.md` recomenda Java para contribuições quando possível; Kotlin é usado onde necessário. Respeite as convenções do pacote e do subsistema que está modificando.
- Não presuma que todos os pacotes legados serão migrados para `pro.sketchware`. Evite migrações em massa ou renomeações que alterem nomes persistidos, opcodes, caminhos e referências em projetos existentes.
- `public-stable-ids.txt` é passado ao compilador de recursos por `app/build.gradle`. Não remova nem regenere IDs em massa sem necessidade: recursos Android podem ser referenciados por projetos gerados ou instalações existentes.
- Os arquivos de `app/src/main/assets/` podem ser dependências do compilador que roda no dispositivo; atualize extração, versões e caminhos correspondentes ao modificar esses arquivos.
- Evite hardcode de caminhos de armazenamento, nomes de pacote, versões e valores de configuração quando já houver um helper ou setting.
- As mensagens de commit seguem o padrão descrito no `README.md`, por exemplo `feat:`, `fix:`, `refactor:`, `test:`, `docs:` e `chore:`.

## 8. Verificação e depuração

Não há diretórios `src/test` ou `src/androidTest` no estado atual desta branch. Os workflows Android em `.github/workflows/` verificam o projeto por meio de builds Gradle; funcionalidades de editor/build também precisam ser exercitadas em emulador ou dispositivo.

Comandos úteis:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew tasks --all
```

Para depurar:

- Use Logcat para falhas do aplicativo e examine o handler de exceções em `SketchApplication`.
- Para erros na compilação **de um projeto criado no app**, consulte o log exibido por `CompileLogActivity` e `CompileErrorSaver`; isso não é o mesmo que a saída do Gradle que compila o Sketchware Pro.
- `DesignActivity` e `ProjectBuilder` registram progresso e logs da compilação interna. Acompanhe a última etapa reportada para distinguir falhas de recursos, compilação Java/Kotlin, DEX, empacotamento e assinatura.
- Se a build Gradle falhar antes da compilação, confira wrapper, JDK/daemon toolchain e Android SDK antes de investigar o compilador interno.

## 9. CI e releases

Os workflows relevantes estão em `.github/workflows/`, incluindo `android.yml`, `build_apk.yml` e `tags_build.yml`. Eles configuram Java, executam tasks Gradle, injetam configurações de serviços quando aplicável e publicam artefatos/release conforme o workflow e o evento. Verifique o workflow acionado antes de assumir qual variante ou conjunto de secrets será usado.

Antes de preparar uma distribuição:

- configure uma chave de assinatura release própria e proteja-a fora do repositório;
- reveja a configuração atual de `signingConfig` e `minifyEnabled` em `app/build.gradle`;
- confira os secrets e artefatos publicados pelo workflow;
- não publique um build assinado com `testkey.keystore` como se fosse uma chave de distribuição.
