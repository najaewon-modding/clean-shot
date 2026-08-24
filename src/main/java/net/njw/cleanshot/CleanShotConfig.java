package net.njw.cleanshot;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CleanShotConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue COORDINATE_FILENAME;
    public static final ModConfigSpec.BooleanValue CREATE_CSV_LOG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        COORDINATE_FILENAME = builder
                .comment("Include player coordinates in screenshot file names.")
                .translation("njw_clean_shot.configuration.coordinateFilename")
                .define("coordinateFilename", true);

        CREATE_CSV_LOG = builder
                .comment("Create and update cleanshot.csv when taking screenshots.")
                .translation("njw_clean_shot.configuration.createCsvLog")
                .define("createCsvLog", true);

        SPEC = builder.build();
    }

    private CleanShotConfig() {
    }
}