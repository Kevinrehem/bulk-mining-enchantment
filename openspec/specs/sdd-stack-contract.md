# SDD Stack Contract

> This file declares the technology stack and architectural constraints of this project.
> It is read by skills and subagents to make stack-aware decisions.
> Fill in each section and commit this file to version control.

---

## Stack Identity

- **Project Name:** Bulk Mining Enchantment (`bulkminingench`)
- **Primary Language:** Java 25
- **Runtime / Platform:** JVM 25 (`C:/Users/jaca/.jdks/openjdk-25.0.1`) / Minecraft `26.3`
- **Framework:** NeoForge `26.3.0.95` (`neo_form_version=26.3-20261021.000000`, ModDevGradle `2.0.148`)
- **Package Manager:** Gradle

---

## Project Structure

- **Source Root:** `src/main/java` and `src/main/resources`
- **Test Root:** `src/test/java`
- **Entry Point:** `com.bulkminingench.BulkMiningEnchMod`
- **Build Output:** `build/libs/`

---

## Test Runner

- **Command:** `./gradlew test`
- **Test Framework:** JUnit 5 (`org.junit.jupiter:junit-jupiter`, BOM `5.10.2`)
- **Coverage Command:** `./gradlew check`

---

## Code Style

- **Linter:** Java Compiler UTF-8 strict checks
- **Formatter:** Standard Java 25 / Minecraft Clean Code conventions
- **Format Command:** N/A

---

## API Contract (if applicable)

- **API Type:** NeoForge Event Bus & Minecraft Data-Driven Registries (`Registries.ENCHANTMENT`)
- **Spec Location:** `src/main/resources/data/`
- **Base URL (dev):** N/A

---

## Database (if applicable)

- **Engine:** N/A
- **Migration Tool:** N/A
- **Migration Command:** N/A

---

## Notes

- Mandatory Minecraft version: `26.3`
- Mandatory NeoForge version: `26.3.0.95`
- Use `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath(...)`) instead of `ResourceLocation`.
- Must integrate with Enchanting Infuser (`#enchantinginfuser:in_enchanting_infuser` and `#enchantinginfuser:in_advanced_enchanting_infuser`).
