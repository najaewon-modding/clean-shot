package net.njw.cleanshot;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CleanShotConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue COORDINATE_FILENAME;
    public static final ModConfigSpec.BooleanValue CREATE_CSV_LOG;
    public static final ModConfigSpec.BooleanValue ORGANIZE_BY_DATE;
    public static final ModConfigSpec.BooleanValue HIDE_CHAT_IN_SCREENSHOT;

    static {
        ModConfigSpec.Builder builder =
                new ModConfigSpec.Builder();

        COORDINATE_FILENAME = builder
                .comment(
                        "Include player coordinates in screenshot file names."
                )
                .translation(
                        "njw_clean_shot.configuration.coordinateFilename"
                )
                .define(
                        "coordinateFilename",
                        true
                );

        CREATE_CSV_LOG = builder
                .comment(
                        "Create and update cleanshot.csv when taking screenshots."
                )
                .translation(
                        "njw_clean_shot.configuration.createCsvLog"
                )
                .define(
                        "createCsvLog",
                        true
                );

        ORGANIZE_BY_DATE = builder
                .comment(
                        "Organize screenshots into folders by date."
                )
                .translation(
                        "njw_clean_shot.configuration.organizeByDate"
                )
                .define(
                        "organizeByDate",
                        true
                );

        HIDE_CHAT_IN_SCREENSHOT = builder
                .comment(
                        "Hide chat HUD from screenshots unless the chat screen is open."
                )
                .translation(
                        "njw_clean_shot.configuration.hideChatInScreenshot"
                )
                .define(
                        "hideChatInScreenshot",
                        true
                );

        SPEC = builder.build();
    }

    private CleanShotConfig() {
    }
}