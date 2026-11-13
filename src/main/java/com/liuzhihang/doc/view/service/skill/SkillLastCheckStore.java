package com.liuzhihang.doc.view.service.skill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/** Stores only the last successful release-check time, separately from the install record. */
final class SkillLastCheckStore {
    private static final int MAX_BYTES = 4096;
    private final Path file;

    SkillLastCheckStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    Optional<Instant> read() throws IOException {
        SkillInstallMetadataStore.rejectPathLinks(file);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        SkillInstallMetadataStore.rejectSymlink(file);
        long size = Files.size(file);
        if (size <= 0 || size > MAX_BYTES) throw new IOException("Skill check state is empty or exceeds 4 KiB");
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.keySet().equals(Set.of("formatVersion", "lastSuccessfulCheckAt"))
                    || json.get("formatVersion").getAsInt() != 1) {
                throw new IOException("Skill check state format is unsupported");
            }
            return Optional.of(Instant.parse(json.get("lastSuccessfulCheckAt").getAsString()));
        } catch (IOException failure) {
            throw failure;
        } catch (RuntimeException invalid) {
            throw new IOException("Skill check state is malformed", invalid);
        }
    }

    void write(Instant checkedAt) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("formatVersion", 1);
        json.addProperty("lastSuccessfulCheckAt", checkedAt.toString());
        SkillInstallMetadataStore.atomicWrite(file, (json + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
