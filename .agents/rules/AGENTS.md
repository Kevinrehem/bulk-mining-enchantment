# Mandatory Minecraft 26.3 & NeoForge 26.3.0.95 Stack

## Version & Toolchain Invariants (STRICTLY MANDATORY)
- **Minecraft Version:** MUST always be `26.3` (`minecraft_version=26.3`). Never downgrade or substitute with 1.21.x or any other version.
- **NeoForge Loader Version:** MUST always be `26.3.0.95` (`neo_version=26.3.0.95`, `neo_form_version=26.3-20261021.000000`).
- **Gradle Plugin:** Use `net.neoforged.moddev` version `2.0.148`.
- **Java Toolchain:** Use Java 25 (`JavaLanguageVersion.of(25)` / `C:/Users/jaca/.jdks/openjdk-25.0.1`).
- **Minecraft 26.3 API Conventions:**
  - Always use `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath(namespace, path)`) instead of the removed `ResourceLocation`.
  - Enchantments MUST be defined using the Minecraft 26.3 data-driven registry format (`data/<mod_id>/enchantment/<name>.json`) and registered in appropriate tags.
