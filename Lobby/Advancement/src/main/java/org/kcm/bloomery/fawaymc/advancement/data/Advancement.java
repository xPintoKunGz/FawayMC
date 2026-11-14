package org.kcm.bloomery.fawaymc.advancement.data;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class Advancement {
   private final String id;
   private final String category;
   private final String name;
   private final String description;
   private final Material icon;
   private final String tier;
   private final String parentId;
   private final TriggerType triggerType;
   private final int required;
   private final ConfigurationSection triggerData;
   private final ConfigurationSection rewardConfig;
   private final List<String> rewardCommands;
   private final boolean broadcast;

   public Advancement(String id, ConfigurationSection cfg) {
      this.id = id;
      this.category = cfg.getString("category", "general");
      this.name = cfg.getString("name", id);
      this.description = cfg.getString("description", "");
      Material m = Material.matchMaterial(cfg.getString("icon", "STONE"));
      this.icon = m != null ? m : Material.STONE;
      this.tier = cfg.getString("tier", "common");
      this.parentId = cfg.getString("parent", (String)null);
      this.triggerType = TriggerType.fromString(cfg.getString("trigger", "API"));
      this.required = Math.max(1, cfg.getInt("required", 1));
      this.triggerData = cfg.getConfigurationSection("trigger_data");
      this.rewardConfig = cfg.getConfigurationSection("reward");
      this.rewardCommands = cfg.getStringList("reward.commands");
      this.broadcast = cfg.getBoolean("broadcast", false);
   }

   public String getId() {
      return this.id;
   }

   public String getCategory() {
      return this.category;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Material getIcon() {
      return this.icon;
   }

   public String getTier() {
      return this.tier;
   }

   public String getParentId() {
      return this.parentId;
   }

   public TriggerType getTriggerType() {
      return this.triggerType;
   }

   public int getRequired() {
      return this.required;
   }

   public ConfigurationSection getTriggerData() {
      return this.triggerData;
   }

   public ConfigurationSection getRewardConfig() {
      return this.rewardConfig;
   }

   public List<String> getRewardCommands() {
      return this.rewardCommands;
   }

   public boolean isBroadcast() {
      return this.broadcast;
   }
}
