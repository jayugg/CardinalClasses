package com.jayugg.simple_rogue.component;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;

import static com.jayugg.simple_rogue.SimpleRogue.MOD_ID;

public class ModComponents implements EntityComponentInitializer {
    public static final ComponentKey<PlayerSkillComponent> SKILL_COMPONENT = ComponentRegistry.getOrCreate(new Identifier(MOD_ID, "player_skills"), PlayerSkillComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(SKILL_COMPONENT, player -> new PlayerSkillComponent(), RespawnCopyStrategy.ALWAYS_COPY);
    }
}
