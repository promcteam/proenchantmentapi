package com.sucy.enchant.skillapi;

import com.sucy.enchant.api.Cooldowns;
import com.sucy.enchant.api.CustomEnchantment;
import com.sucy.enchant.data.ConfigKey;
import com.sucy.enchant.data.Configuration;
import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.enums.ManaCost;
import com.sucy.skill.api.player.PlayerData;
import com.sucy.skill.api.skills.*;
import mc.promcteam.engine.mccore.config.parse.DataSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * EnchantmentAPI © 2017
 * com.sucy.enchant.skillapi.SkillEnchantment
 */
public class SkillEnchantment extends CustomEnchantment {

    private static final String SKILL = "skill";
    private static final String EFFECT = "effect.";
    private static final String CLICK = "right-click";

    private final Skill skill;

    SkillEnchantment(final String key, final DataSection data) {
        super(key, "No description provided");

        String skillName = data.getString(SKILL, data.getString(EFFECT + SKILL));
        settings.set(SKILL, skillName);
        settings.set(CLICK, true);

        skill = SkillAPI.getSkill(skillName);
        if (skill == null) System.out.println(data.getString(SKILL) + " is not a skill");
        setMaxLevel(skill.getMaxLevel());
        Cooldowns.configure(settings,
                skill.getSettings().getBase(SkillAttribute.COOLDOWN),
                skill.getSettings().getScale(SkillAttribute.COOLDOWN));
    }

    public Skill getSkill() { return skill; }

    @Override
    public void applyEquip(final LivingEntity user, final int level) {
        if (skill instanceof PassiveSkill) {
            ((PassiveSkill) skill).initialize(user, level);
        }
    }

    @Override
    public void applyUnequip(final LivingEntity user, final int level) {
        if (skill instanceof PassiveSkill) {
            ((PassiveSkill) skill).stopEffects(user, level);
        }
    }

    @Override
    public void applyInteractBlock(
            final Player user, final int level, final PlayerInteractEvent event) {

        if (event.getAction() == Action.PHYSICAL) return;
        final boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK;

        if (isRightClick == settings.getBoolean(CLICK, true)) {
            applySkillShot(user, level);
        }
    }

    @Override
    public void applyInteractEntity(
            final Player user, final int level, final PlayerInteractEntityEvent event) {
        if (skill instanceof TargetSkill && event.getRightClicked() instanceof LivingEntity) {
            if (!Cooldowns.onCooldown(this, user, settings, level)) {
                final LivingEntity target = (LivingEntity)event.getRightClicked();
                if (Configuration.using(ConfigKey.SKILL_MANA) && SkillAPI.getSettings().isManaEnabled()) {
                    PlayerData playerData = SkillAPI.getPlayerData(user);
                    double cost = skill.getManaCost(level);
                    if (playerData.getMana() < cost) { return; }
                    if (((TargetSkill) skill).cast(user, target, level, SkillAPI.getSettings().isAlly(user, target))) {
                        playerData.useMana(cost, ManaCost.SKILL_CAST);
                        Cooldowns.start(this, user);
                    }
                } else if (((TargetSkill) skill).cast(user, target, level, SkillAPI.getSettings().isAlly(user, target))) {
                    Cooldowns.start(this, user);
                }
            }
        } else applySkillShot(user, level);
    }

    private void applySkillShot(final Player user, final int level) {
        if (skill instanceof SkillShot && !Cooldowns.onCooldown(this, user, settings, level)) {
            if (Configuration.using(ConfigKey.SKILL_MANA) && SkillAPI.getSettings().isManaEnabled()) {
                PlayerData playerData = SkillAPI.getPlayerData(user);
                double cost = skill.getManaCost(level);
                if (playerData.getMana() < cost) { return; }
                if (((SkillShot) skill).cast(user, level)) {
                    playerData.useMana(cost, ManaCost.SKILL_CAST);
                    Cooldowns.start(this, user);
                }
            } else if (((SkillShot) skill).cast(user, level)) {
                Cooldowns.start(this, user);
            }
        }
    }

    static final String SAVE_FOLDER = "enchant/skill/";

    @Override
    public String getSaveFolder() {
        return SAVE_FOLDER;
    }
}
