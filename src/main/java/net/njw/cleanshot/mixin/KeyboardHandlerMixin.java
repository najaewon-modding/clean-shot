package net.njw.cleanshot.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.njw.cleanshot.CleanShot;
import net.njw.cleanshot.CleanShotConfig;
import net.njw.cleanshot.ScreenshotCaptureHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Inject(
            method = "keyPress",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cleanShot$interceptScreenshot(
            long handle,
            int action,
            KeyEvent event,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        // 현재 Minecraft 창에서 발생한 입력만 처리한다.
        if (handle != minecraft.getWindow().handle()) {
            return;
        }

        // 키를 실제로 누른 순간만 처리한다.
        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        // F2 또는 사용자가 지정한 Screenshot 키가 아니면 무시한다.
        if (!minecraft.options.keyScreenshot.matches(event)) {
            return;
        }

        /*
         * Ctrl + Screenshot의 vanilla panorama/debug 기능은
         * CleanShot이 가로채지 않는다.
         */
        if (event.hasControlDownWithQuirk()
                && SharedConstants.DEBUG_PANORAMA_SCREENSHOT) {
            return;
        }

        /*
         * 설정이 OFF이면 vanilla screenshot을 그대로 사용한다.
         *
         * 따라서 평소 화면에 떠 있는 채팅 HUD도
         * 스크린샷에 그대로 포함된다.
         */
        if (!CleanShotConfig.HIDE_CHAT_IN_SCREENSHOT.get()) {
            CleanShot.LOGGER.debug(
                    "Chat hiding disabled. Using vanilla screenshot."
            );

            return;
        }

        /*
         * T 또는 / 를 눌러 ChatScreen이 열린 경우.
         *
         * 이 경우에는 사용자가 채팅 기록과 입력창을
         * 의도적으로 보고 있는 상황이므로
         * vanilla screenshot을 그대로 사용한다.
         */
        if (minecraft.screen instanceof ChatScreen) {
            CleanShot.LOGGER.debug(
                    "ChatScreen is open. Using vanilla screenshot."
            );

            return;
        }

        /*
         * 일반 게임 화면 + 채팅 숨김 ON
         *
         * vanilla의 즉시 Screenshot.grab()을 실행하지 않고,
         * CleanShot이 다음 렌더 프레임에서 채팅 HUD를 제거한 뒤
         * 스크린샷을 찍도록 요청한다.
         */
        CleanShot.LOGGER.debug(
                "Intercepting screenshot to hide chat HUD."
        );

        ScreenshotCaptureHandler.requestScreenshot();

        /*
         * KeyboardHandler.keyPress() 전체를 여기서 종료한다.
         *
         * 따라서 아래쪽에 있는 vanilla Screenshot.grab()은
         * 실행되지 않는다.
         */
        ci.cancel();
    }
}