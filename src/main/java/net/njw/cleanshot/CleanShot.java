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

@Mod(value = CleanShot.MODID, dist = Dist.CLIENT)
public class CleanShot {

    public static final String MODID = "njw_clean_shot";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter SCREENSHOT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public CleanShot(IEventBus modEventBus, ModContainer modContainer) {

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
                ConfigurationScreen::new
        );

        // ------------------------------------------------------------
        // Events
        // ------------------------------------------------------------

        NeoForge.EVENT_BUS.addListener(this::onScreenshot);

        LOGGER.info("CleanShot initialized.");
    }

    private void onScreenshot(ScreenshotEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        // 월드 밖에서는 vanilla screenshot 동작을 그대로 사용한다.
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // ------------------------------------------------------------
        // 촬영 시각
        //
        // 날짜와 파일명의 시간이 자정 경계에서 서로 달라지는 것을
        // 방지하기 위해 LocalDateTime을 한 번만 얻는다.
        // ------------------------------------------------------------

        LocalDateTime now = LocalDateTime.now();

        String date = now.format(DATE_FORMAT);
        String timestamp = now.format(SCREENSHOT_TIME_FORMAT);

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

        // ------------------------------------------------------------
        // Screenshot root
        // ------------------------------------------------------------

        File originalFile = event.getScreenshotFile();

        File screenshotRootDirectory =
                originalFile.getParentFile();

        // ------------------------------------------------------------
        // 실제 저장 폴더 결정
        // ------------------------------------------------------------

        File screenshotDirectory;

        if (CleanShotConfig.ORGANIZE_BY_DATE.get()) {
            screenshotDirectory =
                    new File(screenshotRootDirectory, date);
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
        // 파일명 결정
        // ------------------------------------------------------------

        String fileName;

        if (CleanShotConfig.COORDINATE_FILENAME.get()) {

            fileName = String.format(
                    Locale.ROOT,
                    "%s_[%d,%d,%d].png",
                    timestamp,
                    blockPos.getX(),
                    blockPos.getY(),
                    blockPos.getZ()
            );

        } else {

            // 좌표 파일명 옵션이 OFF인 경우
            // vanilla가 생성한 원래 파일명을 사용한다.
            fileName = originalFile.getName();
        }

        File newFile = getUniqueFile(
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
        // PNG 저장
        // ------------------------------------------------------------

        try {
            /*
             * ScreenshotEvent가 vanilla 저장 전에 발생하므로
             * CleanShot에서 직접 PNG를 저장한다.
             */
            event.getImage().writeToFile(newFile);

            /*
             * 저장된 PNG에 CleanShot metadata 추가
             */
            PngMetadataWriter.addMetadata(
                    newFile,
                    metadata
            );

            /*
             * CleanShot에서 이미 저장했으므로
             * vanilla 저장은 취소한다.
             */
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
                    newFile
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

                /*
                 * CSV는 날짜별 폴더 안이 아니라
                 * 항상 screenshots 루트에 하나만 유지한다.
                 *
                 * CSV의 File 필드에는 screenshots 폴더 기준
                 * 상대 경로를 기록한다.
                 */

                String csvFilePath;

                if (CleanShotConfig.ORGANIZE_BY_DATE.get()) {
                    csvFilePath =
                            date + "/" + newFile.getName();
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
                     * PNG는 이미 정상 저장되었으므로
                     * CSV 실패 때문에 screenshot 자체를
                     * 실패 처리하지 않는다.
                     */
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
        File file = new File(
                directory,
                fileName
        );

        if (!file.exists()) {
            return file;
        }

        String baseName;

        if (fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
            baseName =
                    fileName.substring(
                            0,
                            fileName.length() - 4
                    );
        } else {
            baseName = fileName;
        }

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