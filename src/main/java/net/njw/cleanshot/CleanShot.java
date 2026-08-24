package net.njw.cleanshot;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CleanShot.MODID)
public class CleanShot {

    public static final String MODID = "njw_clean_shot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CleanShot(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("CleanShot initialized.");
    }
}