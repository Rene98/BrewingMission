package me.renes.brewingmission;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.missions.Mission;
import com.bgsoftware.superiorskyblock.api.missions.MissionLoadException;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class brewingMission extends Mission<brewingMission.BrewingTracker> implements Listener {

  private static final SuperiorSkyblock superiorSkyblock = SuperiorSkyblockAPI.getSuperiorSkyblock();

  private static final Pattern percentagePattern = Pattern.compile("(.*)\\{percentage_(.+?)}(.*)"),
          valuePattern = Pattern.compile("(.*)\\{value_(.+?)}(.*)");

  private final Map<String, Integer> potionToBrew = new HashMap<>();

  private JavaPlugin plugin;

  private NamespacedKey brewed;



  @Override
  public void load(JavaPlugin plugin, ConfigurationSection section) throws MissionLoadException {
    this.plugin = plugin;

    if(!section.contains("brewings"))
      throw new MissionLoadException("You must have the \"brewings\" section in the config.");

    for(String key : section.getConfigurationSection("brewings").getKeys(false)){
      String type = section.getString("brewings." + key + ".potionType");
      boolean extended = section.getBoolean("brewings." + key + ".extended");
      boolean upgraded = section.getBoolean("brewings." + key + ".upgraded");
      boolean splash = section.getBoolean("brewings." + key + ".splash");
      int amount = section.getInt("brewings." + key + ".amount", 1);
      PotionType potionType;
      if(!(type.equalsIgnoreCase("any-potion-extended") ||
              type.equalsIgnoreCase("any-potion-upgraded")||
              type.equalsIgnoreCase("any-potion-splash"))) {
        try {
          PotionType.valueOf(type);
        } catch (IllegalArgumentException ex) {
          throw new MissionLoadException("Invalid potionType: " + type + ".");
        }
        String potionString = type + "|" + extended + "|" + upgraded+ "|" + splash;
        potionToBrew.put(potionString.toUpperCase(), amount);
      } else {
        potionToBrew.put(type.toUpperCase(), amount);
      }
      //plugin.getLogger().info("LOADING [" + key + "] " + amount + "x " + potionString.toUpperCase());
    }

    Bukkit.getPluginManager().registerEvents(this, plugin);

    brewed = new NamespacedKey(plugin, "brewed");

    for(String key : potionToBrew.keySet()) {
      plugin.getLogger().info("[" + potionToBrew.get(key) + "] > " + key);
    }
    setClearMethod(brewingTracker -> brewingTracker.brewedPotions.clear());
  }

  @Override
  public double getProgress(SuperiorPlayer superiorPlayer) {
    BrewingTracker brewingTracker = get(superiorPlayer);

    if(brewingTracker == null)
      return 0.0;

    int requiredItems = 0;
    int interactions = 0;

    for(Map.Entry<String, Integer> entry : this.potionToBrew.entrySet()){
      requiredItems += entry.getValue();
      interactions += Math.min(brewingTracker.getBrews(entry.getKey()), entry.getValue());
    }

    return (double) interactions / requiredItems;
  }

  @Override
  public int getProgressValue(SuperiorPlayer superiorPlayer) {
    BrewingTracker brewingTracker = get(superiorPlayer);

    if(brewingTracker == null)
      return 0;

    int interactions = 0;

    for(Map.Entry<String, Integer> entry : this.potionToBrew.entrySet())
      interactions += Math.min(brewingTracker.getBrews(entry.getKey()), entry.getValue());

    return interactions;
  }

  @Override
  public void onComplete(SuperiorPlayer superiorPlayer) {
    onCompleteFail(superiorPlayer);
  }

  @Override
  public void onCompleteFail(SuperiorPlayer superiorPlayer) {
    clearData(superiorPlayer);
  }

  @Override
  public void saveProgress(ConfigurationSection section) {
    for(Map.Entry<SuperiorPlayer, BrewingTracker> entry : entrySet()){
      String uuid = entry.getKey().getUniqueId().toString();
      int index = 0;
      for(Map.Entry<String, Integer> craftedEntry : entry.getValue().brewedPotions.entrySet()){
        section.set(uuid + "." + index + ".potionType", craftedEntry.getKey());
        section.set(uuid + "." + index + ".amount", craftedEntry.getValue());
        index++;
      }
    }
  }

  @Override
  public void loadProgress(ConfigurationSection section) {
    for(String uuid : section.getKeys(false)){
      if(uuid.equals("players"))
        continue;

      BrewingTracker brewingTracker = new BrewingTracker();
      UUID playerUUID = UUID.fromString(uuid);
      SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(playerUUID);

      insertData(superiorPlayer, brewingTracker);

      for (String key : section.getConfigurationSection(uuid).getKeys(false)) {
        String potionType = section.getString(uuid + "." + key + ".potionType");
        int amount = section.getInt(uuid + "." + key + ".amount");
        brewingTracker.brewedPotions.put(potionType, amount);
      }
    }
  }

  @Override
  public void formatItem(SuperiorPlayer superiorPlayer, ItemStack itemStack) {
    BrewingTracker brewingTracker = getOrCreate(superiorPlayer, s -> new BrewingTracker());

    ItemMeta itemMeta = itemStack.getItemMeta();

    if(itemMeta.hasDisplayName())
      itemMeta.setDisplayName(parsePlaceholders(brewingTracker, itemMeta.getDisplayName()));

    if(itemMeta.hasLore()){
      List<String> lore = new ArrayList<>();
      for(String line : itemMeta.getLore())
        lore.add(parsePlaceholders(brewingTracker, line));
      itemMeta.setLore(lore);
    }
    itemStack.setItemMeta(itemMeta);
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onBrew(BrewEvent e) {
    int index = 0;
    for(ItemStack item : e.getContents()) {
      ItemMeta im = item.getItemMeta();
      im.getPersistentDataContainer().set(brewed, PersistentDataType.STRING, "brewStand");
      item.setItemMeta(im);
      e.getContents().setItem(index, item);
      index++;
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent e) {
    if (e.getClickedInventory() == null || e.getClickedInventory().getType() != InventoryType.BREWING)
      return;

    if (e.getRawSlot() < 0 || e.getRawSlot() > 2) {
      return;
    }
    if (!e.getCurrentItem().getType().toString().contains("POTION")) {
      return;
    }
    if (e.getCurrentItem().getItemMeta().getPersistentDataContainer().getOrDefault(brewed, PersistentDataType.STRING,
            "brewStand").equalsIgnoreCase("player")) {
      return;
    }
    PotionMeta potionMeta = (PotionMeta) e.getCurrentItem().getItemMeta();
    PotionData potionData = potionMeta.getBasePotionData();
    String type = potionData.getType().toString();
    boolean extended = potionData.isExtended();
    boolean upgraded = potionData.isUpgraded();
    boolean splash = e.getCurrentItem().getType() == Material.SPLASH_POTION;
    boolean needsTracking = potionToBrew.containsKey((type + "|" + extended + "|" + upgraded + "|" + splash).toUpperCase())
            || (extended && potionToBrew.containsKey("any-potion-extended".toUpperCase()))
            || (upgraded && potionToBrew.containsKey("any-potion-upgraded".toUpperCase()))
            || (upgraded && potionToBrew.containsKey("any-potion-splash".toUpperCase()));
    SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(e.getWhoClicked().getUniqueId());
    if (needsTracking && superiorSkyblock.getMissions().canCompleteNoProgress(superiorPlayer, this)) {
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        BrewingTracker brewingTracker = getOrCreate(superiorPlayer, s -> new BrewingTracker());
        brewingTracker.trackItem(type + "|" + extended + "|" + upgraded + "|" + splash);
      }, 1L);
    }
    ItemMeta im = e.getCurrentItem().getItemMeta();
    im.getPersistentDataContainer().set(brewed, PersistentDataType.STRING, "player");
    e.getCurrentItem().setItemMeta(im);
  }

  private String parsePlaceholders(BrewingTracker entityTracker, String line){
    Matcher matcher = percentagePattern.matcher(line);

    if(matcher.matches()){
      try {
        String requiredBlock = matcher.group(2).toUpperCase();
        Optional<Map.Entry<String, Integer>> entry =
                potionToBrew.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(requiredBlock)).findAny();
        if (entry.isPresent()) {
          int percentage = Math.round(entityTracker.getBrews(requiredBlock)/entry.get().getValue()*100);
          if(percentage > 100) {
            percentage = 100;
          }
          line = line.replace("{percentage_" + matcher.group(2) + "}",
                  "" + percentage);
        }
      }catch(Exception ignored){}
    }
    if((matcher = valuePattern.matcher(line)).matches()){
      try {
        String requiredBlock = matcher.group(2).toUpperCase();
        Optional<Map.Entry<String, Integer>> entry =
                potionToBrew.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(requiredBlock)).findAny();
        if (entry.isPresent()) {
          int brews = entityTracker.getBrews(requiredBlock);
          if(brews > potionToBrew.get(requiredBlock)) {
            brews = potionToBrew.get(requiredBlock);
          }
          line = line.replace("{value_" + matcher.group(2) + "}", "" + brews);
        }
      }catch(Exception ignored){}
    }

    return ChatColor.translateAlternateColorCodes('&', line);
  }

  public static class BrewingTracker {

    private final Map<String, Integer> brewedPotions = new HashMap<>();

    void trackItem(String potion){
      String[] splitPot = potion.split("\\|");
      String potionType = splitPot[0];
      boolean extended = Boolean.parseBoolean(splitPot[1]);
      boolean upgraded = Boolean.parseBoolean(splitPot[2]);
      boolean splash = Boolean.parseBoolean(splitPot[3]);
      if(extended) {
        brewedPotions.put("ANY-POTION-EXTENDED", brewedPotions.getOrDefault("ANY-POTION-EXTENDED", 0) + 1);
      }
      if(upgraded) {
        brewedPotions.put("ANY-POTION-UPGRADED", brewedPotions.getOrDefault("ANY-POTION-UPGRADED", 0) + 1);
      }
      if(splash) {
        brewedPotions.put("ANY-POTION-SPLASH", brewedPotions.getOrDefault("ANY-POTION-SPLASH", 0) + 1);
      }
      brewedPotions.put(potion.toUpperCase(), brewedPotions.getOrDefault(potion.toUpperCase(), 0) + 1);
    }

    int getBrews(String potionType){
      return brewedPotions.getOrDefault(potionType.toUpperCase(), 0);
    }

  }

}
