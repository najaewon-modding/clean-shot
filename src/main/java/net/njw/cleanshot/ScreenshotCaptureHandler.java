package net.njw.cleanshot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class ScreenshotCaptureHandler {

    private static int pendingScreenshots = 0;

    private ScreenshotCaptureHandler() {
    }

    /**
     * 채팅 HUD가 제거된 다음 렌더 프레임에서
     * 스크린샷을 촬영하도록 요청한다.
     */
    public static void requestScreenshot() {
        pendingScreenshots++;

        CleanShot.LOGGER.debug(
                "Screenshot requested. Pending screenshots: {}",
                pendingScreenshots
        );
    }

    /**
     * 스크린샷을 촬영할 프레임에서는
     * vanilla CHAT HUD layer만 렌더링하지 않는다.
     *
     * 핫바, 체력, 갑옷, 크로스헤어 등의 다른 HUD는
     * 그대로 렌더링된다.
     */
    public static void onRenderGuiLayer(
            RenderGuiLayerEvent.Pre event
    ) {
        if (pendingScreenshots <= 0) {
            return;
        }

        if (!CleanShotConfig.HIDE_CHAT_IN_SCREENSHOT.get()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        /*
         * 혹시 screenshot 요청과 실제 렌더링 사이에
         * ChatScreen이 열린 경우에는 채팅을 숨기지 않는다.
         */
        if (minecraft.screen instanceof ChatScreen) {
            return;
        }

        /*
         * 평소 화면 왼쪽 아래에 표시되는
         *
         * - 일반 채팅
         * - 시스템 메시지
         * - 명령어 결과
         * - 스크린샷 저장 메시지
         *
         * 등이 모두 이 CHAT layer에 포함된다.
         */
        if (VanillaGuiLayers.CHAT.equals(event.getName())) {

            CleanShot.LOGGER.debug(
                    "Hiding vanilla chat HUD for screenshot."
            );

            event.setCanceled(true);
        }
    }

    /**
     * 채팅 HUD가 제외된 프레임의 렌더링이 끝난 직후
     * 실제 framebuffer를 캡처한다.
     */
    public static void onRenderFramePost(
            RenderFrameEvent.Post event
    ) {
        if (pendingScreenshots <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        int screenshotsToTake = pendingScreenshots;

        /*
         * Screenshot.grab()이 진행되는 동안
         * 다음 프레임까지 pending 상태가 유지되지 않도록
         * 먼저 초기화한다.
         */
        pendingScreenshots = 0;

        CleanShot.LOGGER.debug(
                "Capturing {} CleanShot screenshot(s).",
                screenshotsToTake
        );

        for (int i = 0; i < screenshotsToTake; i++) {

            Screenshot.grab(
                    minecraft.gameDirectory,
                    minecraft.getMainRenderTarget(),
                    message -> minecraft.execute(
                            () -> minecraft.gui
                                    .getChat()
                                    .addClientSystemMessage(message)
                    )
            );
        }
    }
}