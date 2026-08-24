package net.njw.cleanshot;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ScreenshotEvent;
import org.slf4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mod(value = CleanShot.MODID, dist = Dist.CLIENT)
public class CleanShot {

    public static final String MODID = "njw_clean_shot";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DateTimeFormatter SCREENSHOT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    public CleanShot(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::onScreenshot);

        LOGGER.info("CleanShot initialized.");
    }

    private void onScreenshot(ScreenshotEvent event) {
        Minecraft minecraft = Minecraft.getInstance();

        // 월드에 들어가 있지 않으면 좌표를 얻을 수 없으므로
        // 기존 스크린샷 파일명을 그대로 사용한다.
        if (minecraft.player == null) {
            return;
        }

        BlockPos pos = minecraft.player.blockPosition();

        String timestamp = LocalDateTime.now().format(SCREENSHOT_TIME_FORMAT);

        String fileName = String.format(
                "%s_[%d,%d,%d].png",
                timestamp,
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );

        File originalFile = event.getScreenshotFile();
        File screenshotDirectory = originalFile.getParentFile();

        File newFile = getUniqueFile(screenshotDirectory, fileName);

        event.setScreenshotFile(newFile);

        LOGGER.info(
                "Screenshot: {} [{}, {}, {}]",
                newFile.getName(),
                pos.getX(),
                pos.getY(),
                pos.getZ()
        );
    }

    private static File getUniqueFile(File directory, String fileName) {
        File file = new File(directory, fileName);

        if (!file.exists()) {
            return file;
        }

        String baseName = fileName.substring(0, fileName.length() - 4);

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