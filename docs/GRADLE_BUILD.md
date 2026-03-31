# Build com Gradle (Java + CMake) — Windows e Linux

Este repositório passou a ter um build **Gradle** para substituir o fluxo anterior baseado em Ant/scripts, **sem reimplementar** o linking do C/C++ (isso continua no **CMake**).

## O que mudou

- **Novo**: `build.gradle` e `settings.gradle` na raiz.
- **Java**:
  - Compila `java/org/cef/**` (layout não padrão).
  - Gera `out/<platform>/jcef.jar` e `out/<platform>/jcef-tests.jar`.
  - Gera javadoc em `out/docs`.
- **Nativo (C/C++)**:
  - O Gradle **orquestra** o CMake usando o diretório fixo `jcef_build/`.
  - O linking/cópia de binários do CEF continuam sendo feitos pelo CMake (`CMakeLists.txt` e `native/CMakeLists.txt`).
- **Distribuição**:
  - Task Gradle cria `binary_distrib/<platform>` copiando jars, testes, docs e a saída nativa.

## Pré-requisitos

- **Java (JDK)**: você pode usar JDK moderno (ex.: 17+). O build gera bytecode **Java 8**.
- **CMake**: 3.21+.
- **Toolchain nativa**:
  - Windows: Visual Studio 2022 (MSVC).
  - Linux: Ninja ou Make + GCC/Clang.
- **Python (para o CMake)**:
  - O CMake deste repo executa scripts Python durante a configuração/build.
- **Google Cloud SDK (`gsutil`) no PATH (para o CMake)**:
  - O CMake usa `gsutil` para baixar `clang-format` (bucket público).

### Quanto ocupa instalar Python?

Isso varia muito por instalador/OS, mas como referência:

- **Python 3.x (Windows, installer “completo”)**: ~25–60 MB.
Na prática, o “peso” maior costuma ser o Python em si e, se você usar, um `venv` (que pode adicionar mais alguns MB dependendo do que for instalado nele).

## Como usar

> Todos os comandos abaixo são executados na raiz do repo.

### Listar tasks

```bash
./gradlew tasks --all
```

### Build Java (jars)

```bash
./gradlew clean jar jcefTestsJar stageOut
```

Saída:
- `out/<platform>/jcef.jar`
- `out/<platform>/jcef-tests.jar`

### Gerar javadoc

```bash
./gradlew jcefJavadoc
```

Saída:
- `out/docs/`

### Configurar/build nativo via CMake

Configurar:

```bash
./gradlew cmakeConfigure
```

#### Build Linux x64 via Docker (recomendado para ambiente limpo)

1. Crie a imagem Docker (executar na raiz do repo):

```bash
docker build -t browser4j-cef-linux64 -f Dockerfile.linux64 .
```

2. Execute o build dentro do container:

```bash
docker run --rm -v "$(pwd)":/workspace -w /workspace browser4j-cef-linux64 \
  bash -lc "./gradlew cmakeConfigure nativeRelease --no-daemon"
```

3. Opcional: gerar distribuição binária:

```bash
docker run --rm -v "$(pwd)":/workspace -w /workspace browser4j-cef-linux64 \
  bash -lc "./gradlew makeDistrib --no-daemon"
```

> Observação: por enquanto o foco é Linux x64, então o Windows pode ser feito localmente com o fluxo já existente.

Build (Release):

```bash
./gradlew nativeRelease
```

### Publicar no Maven (hyperpowered)

Este projeto já está configurado em `build.gradle` para publicar via `maven-publish`.

- releases: `https://maven.dev.hyperpowered.net/releases`
- snapshots: `https://maven.dev.hyperpowered.net/snapshots`

Defina as variáveis de ambiente antes de chamar o publish:

```bash
export HYPER_MAVEN_USERNAME="seu_usuario"
export HYPER_MAVEN_PASSWORD="sua_senha"
```

Para publicar a versão atual:

```bash
./gradlew publish --no-daemon
```

Para publicar apenas `mavenJava` (relacionado):

```bash
./gradlew publishMavenJavaPublicationToHyperpoweredMavenRepository --no-daemon
```

#### Se o `cmakeConfigure` falhar por falta de `gsutil`

Se o erro indicar que `gsutil` não foi encontrado, você tem duas opções:

**Opção A (recomendada)**: abrir o build em um shell onde o Google Cloud SDK já está no `PATH` (ou adicionar o Cloud SDK ao `PATH` do seu ambiente).

```bash
gsutil version
```

**Opção B**: se o CMake estiver usando um Python errado (ou você quiser garantir qual Python ele usa), aponte explicitamente via propriedade:

```bash
./gradlew cmakeConfigure -PpythonExecutable="C:/caminho/do/python.exe"
```

### Criar distribuição (equivalente ao `make_distrib.*`)

```bash
./gradlew makeDistrib
```

Saída:
- `binary_distrib/<platform>/...`

## Plataforma (`platform`)

O Gradle tenta inferir automaticamente:
- Windows 64-bit → `win64`
- Windows 32-bit → `win32`
- Linux 64-bit → `linux64`
- Linux 32-bit → `linux32`

Você pode forçar:

```bash
./gradlew stageOut -Pplatform=win64
./gradlew makeDistrib -Pplatform=linux64
```

## Nota sobre fontes específicas de macOS

No Windows/Linux, classes específicas de macOS (ex.: `org/cef/**/mac/**`) são excluídas do build Java para evitar imports `sun.*` que não existem fora do macOS.

