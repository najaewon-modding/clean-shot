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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Mod(
        value = CleanShot.MODID,
        dist = Dist.CLIENT
)
public class CleanShot {

    public static final String MODID =
            "njw_clean_shot";

    public static final Logger LOGGER =
            LogUtils.getLogger();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd"
            );

    private static final DateTimeFormatter SCREENSHOT_TIME_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd_HH.mm.ss"
            );

    public CleanShot(
            IEventBus modEventBus,
            ModContainer modContainer
    ) {

        // ------------------------------------------------------------
        // Client config
        // ------------------------------------------------------------

        modContainer.registerConfig(
                ModConfig.Type.CLIENT,
                CleanShotConfig.SPEC
        );

        // Mods -> CleanShot -> Config
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) ->
                        new ConfigurationScreen(
                                container,
                                parent
                        )
        );

        // ------------------------------------------------------------
        // Screenshot
        // ------------------------------------------------------------

        NeoForge.EVENT_BUS.addListener(
                this::onScreenshot
        );

        // ------------------------------------------------------------
        // Chat hiding / delayed screenshot
        // ------------------------------------------------------------

        NeoForge.EVENT_BUS.addListener(
                ScreenshotCaptureHandler::onRenderGuiLayer
        );

        NeoForge.EVENT_BUS.addListener(
                ScreenshotCaptureHandler::onRenderFramePost
        );

        LOGGER.info(
                "CleanShot initialized."
        );
    }

    private void onScreenshot(
            ScreenshotEvent event
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        /*
         * 월드 밖에서는 위치 정보가 없으므로
         * vanilla screenshot 처리를 그대로 사용한다.
         */
        if (minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        // ------------------------------------------------------------
        // 촬영 시각
        // ------------------------------------------------------------

        LocalDateTime now =
                LocalDateTime.now();

        String date =
                now.format(DATE_FORMAT);

        String timestamp =
                now.format(
                        SCREENSHOT_TIME_FORMAT
                );

        // ------------------------------------------------------------
        // 플레이어 정보
        // ------------------------------------------------------------

        BlockPos blockPos =
                minecraft.player.blockPosition();

        double x =
                minecraft.player.getX();

        double y =
                minecraft.player.getY();

        double z =
                minecraft.player.getZ();

        float yaw =
                minecraft.player.getYRot();

        float pitch =
                minecraft.player.getXRot();

        String dimension =
                minecraft.level
                        .dimension()
                        .identifier()
                        .toString();

        // ------------------------------------------------------------
        // screenshots root
        // ------------------------------------------------------------

        File originalFile =
                event.getScreenshotFile();

        File screenshotRootDirectory =
                originalFile.getParentFile();

        // ------------------------------------------------------------
        // 저장 폴더
        // ------------------------------------------------------------

        File screenshotDirectory;

        if (CleanShotConfig.ORGANIZE_BY_DATE.get()) {

            screenshotDirectory =
                    new File(
                            screenshotRootDirectory,
                            date
                    );

        } else {

            screenshotDirectory =
                    screenshotRootDirectory;
        }

        try {
            Files.createDirectories(
                    screenshotDirectory.toPath()
            );

        } catch (IOException e) {

            event.setCanceled(true);

            event.setResultMessage(
                    Component.translatable(
                            "message.njw_clean_shot.screenshot.failed"
                    )
            );

            LOGGER.error(
                    "Failed to create screenshot directory: {}",
                    screenshotDirectory,
                    e
            );

            return;
        }

        // ------------------------------------------------------------
        // 파일 이름
        // ------------------------------------------------------------

        String fileName;

        if (CleanShotConfig.COORDINATE_FILENAME.get()) {

            fileName =
                    String.format(
                            Locale.ROOT,
                            "%s_[%d,%d,%d].png",
                            timestamp,
                            blockPos.getX(),
                            blockPos.getY(),
                            blockPos.getZ()
                    );

        } else {

            fileName =
                    originalFile.getName();
        }

        File newFile =
                getUniqueFile(
                        screenshotDirectory,
                        fileName
                );

        // ------------------------------------------------------------
        // PNG metadata
        // ------------------------------------------------------------

        Map<String, String> metadata =
                new LinkedHashMap<>();

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
        // CleanShot이 파일 저장을 직접 담당한다.
        // ------------------------------------------------------------

        event.setScreenshotFile(newFile);
        event.setCanceled(true);

        try {

            // Minecraft NativeImage 저장
            event.getImage()
                    .writeToFile(newFile);

            // PNG 내부 CleanShot metadata 기록
            PngMetadataWriter.addMetadata(
                    newFile,
                    metadata
            );

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
                    newFile
            );

            LOGGER.info(
                    "CleanShot.Position = {}",
                    metadata.get(
                            "CleanShot.Position"
                    )
            );

            LOGGER.info(
                    "CleanShot.Dimension = {}",
                    metadata.get(
                            "CleanShot.Dimension"
                    )
            );

            LOGGER.info(
                    "CleanShot.Yaw = {}",
                    metadata.get(
                            "CleanShot.Yaw"
                    )
            );

            LOGGER.info(
                    "CleanShot.Pitch = {}",
                    metadata.get(
                            "CleanShot.Pitch"
                    )
            );

            // --------------------------------------------------------
            // CSV
            // --------------------------------------------------------

            if (CleanShotConfig.CREATE_CSV_LOG.get()) {

                String csvFilePath;

                if (CleanShotConfig.ORGANIZE_BY_DATE.get()) {

                    csvFilePath =
                            date
                                    + "/"
                                    + newFile.getName();

                } else {

                    csvFilePath =
                            newFile.getName();
                }

                try {

                    CsvLogWriter.append(
                            screenshotRootDirectory.toPath(),
                            csvFilePath,
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

                    /*
                     * PNG 저장에는 성공했으므로
                     * CSV 오류 때문에 screenshot 전체를
                     * 실패 처리하지 않는다.
                     */
                    LOGGER.warn(
                            "Screenshot was saved, but failed to update cleanshot.csv.",
                            e
                    );
                }
            }

        } catch (IOException e) {

            event.setResultMessage(
                    Component.translatable(
                            "message.njw_clean_shot.screenshot.failed"
                    )
            );

            LOGGER.error(
                    "Failed to save screenshot with CleanShot metadata.",
                    e
            );

        } finally {

            /*
             * ScreenshotEvent를 취소하면 vanilla 쪽에서
             * NativeImage 저장/정리 과정을 수행하지 않으므로
             * CleanShot이 직접 닫는다.
             */
            event.getImage().close();
        }
    }

    private static File getUniqueFile(
            File directory,
            String fileName
    ) {
        File file =
                new File(
                        directory,
                        fileName
                );

        if (!file.exists()) {
            return file;
        }

        String baseName;

        if (fileName
                .toLowerCase(Locale.ROOT)
                .endsWith(".png")) {

            baseName =
                    fileName.substring(
                            0,
                            fileName.length() - 4
                    );

        } else {

            baseName =
                    fileName;
        }

        int index = 2;

        while (file.exists()) {

            file =
                    new File(
                            directory,
                            baseName
                                    + "_"
                                    + index
                                    + ".png"
                    );

            index++;
        }

        return file;
    }
}