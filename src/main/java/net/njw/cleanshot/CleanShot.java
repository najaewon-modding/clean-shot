package net.njw.cleanshot;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Mod(value = CleanShot.MODID, dist = Dist.CLIENT)
public class CleanShot {

    public static final String MODID = "njw_clean_shot";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DateTimeFormatter SCREENSHOT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public CleanShot(IEventBus modEventBus, ModContainer modContainer) {

        // Client config 등록
        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                CleanShotConfig.SPEC
        );

        // Mods -> CleanShot -> Config 화면 등록
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) ->
                        new ConfigurationScreen(container, parent)
        );

        // Screenshot event
        NeoForge.EVENT_BUS.addListener(this::onScreenshot);

        LOGGER.info("CleanShot initialized.");
    }

    private void onScreenshot(ScreenshotEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        // 월드 밖에서는 vanilla screenshot 동작 유지
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // ------------------------------------------------------------
        // 플레이어 정보
        // ------------------------------------------------------------

        BlockPos blockPos = minecraft.player.blockPosition();

        double x = minecraft.player.getX();
        double y = minecraft.player.getY();
        double z = minecraft.player.getZ();

        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();

        String dimension =
                minecraft.level.dimension().identifier().toString();

        File originalFile = event.getScreenshotFile();
        File screenshotDirectory = originalFile.getParentFile();

        // ------------------------------------------------------------
        // 파일명 결정
        // ------------------------------------------------------------

        File newFile;

        if (CleanShotConfig.COORDINATE_FILENAME.get()) {

            String timestamp =
                    LocalDateTime.now().format(SCREENSHOT_TIME_FORMAT);

            String fileName = String.format(
                    Locale.ROOT,
                    "%s_[%d,%d,%d].png",
                    timestamp,
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
            );

            newFile = getUniqueFile(
                    screenshotDirectory,
                    fileName
            );

        } else {
            // 옵션 OFF면 vanilla가 원래 사용하려던 파일명 그대로 사용
            newFile = originalFile;
        }

        // ------------------------------------------------------------
        // PNG metadata
        // ------------------------------------------------------------

        Map<String, String> metadata = new LinkedHashMap<>();

        metadata.put(
                "CleanShot.Position",
                String.format(
                        Locale.ROOT,
                        "%.6f,%.6f,%.6f",
                        x,
                        y,
                        z
                )
        );

        metadata.put(
                "CleanShot.Dimension",
                dimension
        );

        metadata.put(
                "CleanShot.Yaw",
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        yaw
                )
        );

        metadata.put(
                "CleanShot.Pitch",
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        pitch
                )
        );

        // ------------------------------------------------------------
        // PNG 저장
        // ------------------------------------------------------------

        try {
            event.getImage().writeToFile(newFile);

            PngMetadataWriter.addMetadata(
                    newFile,
                    metadata
            );

            event.setScreenshotFile(newFile);
            event.setCanceled(true);

            event.setResultMessage(
                    Component.translatable(
                            "message.njw_clean_shot.screenshot.saved",
                            newFile.getName()
                    )
            );

            // --------------------------------------------------------
            // 로그
            // --------------------------------------------------------

            LOGGER.info(
                    "Saved screenshot: {}",
                    newFile.getName()
            );

            LOGGER.info(
                    "CleanShot.Position = {}",
                    metadata.get("CleanShot.Position")
            );

            LOGGER.info(
                    "CleanShot.Dimension = {}",
                    metadata.get("CleanShot.Dimension")
            );

            LOGGER.info(
                    "CleanShot.Yaw = {}",
                    metadata.get("CleanShot.Yaw")
            );

            LOGGER.info(
                    "CleanShot.Pitch = {}",
                    metadata.get("CleanShot.Pitch")
            );

            // --------------------------------------------------------
            // CSV
            // --------------------------------------------------------

            if (CleanShotConfig.CREATE_CSV_LOG.get()) {

                try {
                    CsvLogWriter.append(
                            screenshotDirectory.toPath(),
                            newFile.getName(),
                            x,
                            y,
                            z,
                            dimension,
                            yaw,
                            pitch
                    );

                    LOGGER.info(
                            "Added screenshot entry to cleanshot.csv."
                    );

                } catch (IOException e) {
                    LOGGER.warn(
                            "Screenshot was saved, but failed to update cleanshot.csv.",
                            e
                    );
                }
            }

        } catch (IOException e) {
            event.setCanceled(true);

            event.setResultMessage(
                    Component.translatable(
                            "message.njw_clean_shot.screenshot.failed"
                    )
            );

            LOGGER.error(
                    "Failed to save screenshot with CleanShot metadata.",
                    e
            );
        }
    }

    private static File getUniqueFile(
            File directory,
            String fileName
    ) {
        File file = new File(directory, fileName);

        if (!file.exists()) {
            return file;
        }

        String baseName =
                fileName.substring(0, fileName.length() - 4);

        int index = 2;

        while (file.exists()) {
            file = new File(
                    directory,
                    baseName + "_" + index + ".png"
            );

            index++;
        }

        return file;
    }
}