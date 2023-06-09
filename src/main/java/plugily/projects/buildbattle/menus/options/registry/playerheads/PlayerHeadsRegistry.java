/*
 *
 * BuildBattle - Ultimate building competition minigame
 * Copyright (C) 2021 Plugily Projects - maintained by Tigerpanzer_02, 2Wild4You and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package plugily.projects.buildbattle.menus.options.registry.playerheads;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import plugily.projects.commonsbox.minecraft.configuration.ConfigUtils;
import plugily.projects.commonsbox.minecraft.item.ItemBuilder;
import plugily.projects.commonsbox.minecraft.item.ItemUtils;
import plugily.projects.commonsbox.minecraft.misc.stuff.ComplementAccessor;
import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.menus.options.OptionsRegistry;
import plugily.projects.buildbattle.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Plajer
 * <p>
 * Created at 23.12.2018
 */
public class PlayerHeadsRegistry {

  private final Main plugin;
  private final Map<HeadsCategory, Inventory> categories = new HashMap<>();

  public PlayerHeadsRegistry(OptionsRegistry registry) {
    this.plugin = registry.getPlugin();
    registerCategories();
  }

  private void registerCategories() {
    FileConfiguration config = ConfigUtils.getConfig(plugin, "heads/mainmenu");
    for(String str : config.getKeys(false)) {
      if(!config.getBoolean(str + ".enabled", true)) {
        continue;
      }
      HeadsCategory category = new HeadsCategory(str);

      category.setItemStack(new ItemBuilder(ItemUtils.getSkull(config.getString(str + ".texture")))
          .name(plugin.getChatManager().colorRawMessage(config.getString(str + ".displayname")))
          .lore(config.getStringList(str + ".lore").stream()
              .map(lore -> lore = plugin.getChatManager().colorRawMessage(lore)).collect(Collectors.toList()))
          .build());
      category.setPermission(config.getString(str + ".permission"));

      Set<ItemStack> playerHeads = new HashSet<>();
      FileConfiguration categoryConfig = ConfigUtils.getConfig(plugin, "heads/menus/" +
          config.getString(str + ".config"));
      for(String path : categoryConfig.getKeys(false)) {
        if(!categoryConfig.getBoolean(path + ".enabled", true)) {
          continue;
        }

        ItemStack stack = ItemUtils.getSkull(categoryConfig.getString(path + ".texture"));
        ItemMeta im = stack.getItemMeta();

        ComplementAccessor.getComplement().setDisplayName(im, plugin.getChatManager().colorRawMessage(categoryConfig.getString(path + ".displayname")));
        ComplementAccessor.getComplement().setLore(im, categoryConfig.getStringList(path + ".lore").stream()
                .map(lore -> lore = plugin.getChatManager().colorRawMessage(lore)).collect(Collectors.toList()));
        stack.setItemMeta(im);
        playerHeads.add(stack);
      }
      Inventory inv = ComplementAccessor.getComplement().createInventory(null, Utils.serializeInt(playerHeads.size() + 1),
          plugin.getChatManager().colorRawMessage(config.getString(str + ".menuname")));
      playerHeads.forEach(inv::addItem);
      inv.addItem(Utils.getGoBackItem());
      category.setInventory(inv);
      categories.put(category, inv);
    }
  }

  public boolean isHeadsMenu(Inventory inventory) {
    for (Inventory inv : categories.values()) {
      if (inv.equals(inventory)) {
        return true;
      }
    }

    return false;
  }

  public Map<HeadsCategory, Inventory> getCategories() {
    return categories;
  }

}
