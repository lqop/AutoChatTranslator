package com.autochattranslator;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(AutoChatTranslator.MODID)
public final class AutoChatTranslator {
    public static final String MODID = "autochattranslator";

    public AutoChatTranslator() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
