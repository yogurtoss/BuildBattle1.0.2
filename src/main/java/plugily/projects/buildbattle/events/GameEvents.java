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

package plugily.projects.buildbattle.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import plugily.projects.commonsbox.minecraft.compat.VersionUtils;
import plugily.projects.commonsbox.minecraft.compat.events.api.CBPlayerInteractEntityEvent;
import plugily.projects.commonsbox.minecraft.compat.events.api.CBPlayerInteractEvent;
import plugily.projects.commonsbox.minecraft.compat.xseries.XMaterial;
import plugily.projects.commonsbox.minecraft.item.ItemUtils;
import plugily.projects.commonsbox.minecraft.misc.stuff.ComplementAccessor;
import plugily.projects.buildbattle.ConfigPreferences;
import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.api.StatsStorage;
import plugily.projects.buildbattle.arena.ArenaManager;
import plugily.projects.buildbattle.arena.ArenaRegistry;
import plugily.projects.buildbattle.arena.ArenaState;
import plugily.projects.buildbattle.arena.ArenaUtils;
import plugily.projects.buildbattle.arena.impl.BaseArena;
import plugily.projects.buildbattle.arena.impl.GuessTheBuildArena;
import plugily.projects.buildbattle.arena.impl.SoloArena;
import plugily.projects.buildbattle.arena.managers.plots.Plot;
import plugily.projects.buildbattle.handlers.items.SpecialItemsManager;
import plugily.projects.buildbattle.user.User;

/**
 * Created by Tom on 17/08/2015.
 */
public class GameEvents implements Listener {

  private final Main plugin;

  public GameEvents(Main plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onSpecialItem(CBPlayerInteractEvent event) {
    if(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    ItemStack itemStack = VersionUtils.getItemInHand(event.getPlayer());
    if(arena == null || !ItemUtils.isItemStackNamed(itemStack)) {
      return;
    }
    String key = plugin.getSpecialItemsManager().getRelatedSpecialItem(itemStack).getName();
    if(key == null) {
      return;
    }
    if(key.equalsIgnoreCase(SpecialItemsManager.SpecialItems.FORCESTART.getName())) {
      event.setCancelled(true);
      ArenaUtils.arenaForceStart(event.getPlayer(), "");
      return;
    }
    if(key.equals(SpecialItemsManager.SpecialItems.LOBBY_LEAVE_ITEM.getName()) || key.equals(SpecialItemsManager.SpecialItems.SPECTATOR_LEAVE_ITEM.getName())) {
      event.setCancelled(true);
      if(plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BUNGEE_ENABLED)) {
        plugin.getBungeeManager().connectToHub(event.getPlayer());
      } else {
        ArenaManager.leaveAttempt(event.getPlayer(), arena);
      }
    }
  }

  @EventHandler
  public void onOpenOptionMenu(CBPlayerInteractEvent event) {
    if(VersionUtils.checkOffHand(event.getHand()) || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
      return;
    }
    if(!ItemUtils.isItemStackNamed(event.getItem())) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null || arena.getArenaState() != ArenaState.IN_GAME || arena instanceof SoloArena && ((SoloArena) arena).isVoting()) {
      return;
    }
    if(!ComplementAccessor.getComplement().getDisplayName(plugin.getOptionsRegistry().getMenuItem().getItemMeta())
        .equalsIgnoreCase(ComplementAccessor.getComplement().getDisplayName(event.getItem().getItemMeta()))) {
      return;
    }
    event.getPlayer().openInventory(plugin.getOptionsRegistry().formatInventory());
  }

  @EventHandler
  public void onPistonExtendEvent(BlockPistonExtendEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        for(Block block : event.getBlocks()) {
          if(!buildPlot.getCuboid().isInWithMarge(block.getLocation(), -1) && buildPlot.getCuboid().isIn(event.getBlock().getLocation())) {
            event.setCancelled(true);
          }
        }
      }
    }
  }

  @EventHandler
  public void onFoodChange(FoodLevelChangeEvent event) {
    if(!(event.getEntity().getType() == EntityType.PLAYER)) {
      return;
    }
    Player player = (Player) event.getEntity();
    if(ArenaRegistry.getArena(player) == null) {
      return;
    }
    event.setCancelled(true);
    player.setFoodLevel(20);
  }

  @EventHandler
  public void onWaterFlowEvent(BlockFromToEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        if(!buildPlot.getCuboid().isIn(event.getToBlock().getLocation()) && buildPlot.getCuboid().isIn(event.getBlock().getLocation())) {
          event.setCancelled(true);
        }
        if(!buildPlot.getCuboid().isInWithMarge(event.getToBlock().getLocation(), -1) && buildPlot.getCuboid().isIn(event.getToBlock().getLocation())) {
          event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler
  public void onTNTExplode(EntityExplodeEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        if(buildPlot.getCuboid() == null)
          continue;
        if(buildPlot.getCuboid().isInWithMarge(event.getEntity().getLocation(), 5)) {
          event.blockList().clear();
          event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler
  public void onTNTInteract(CBPlayerInteractEvent event) {
    if(event.getClickedBlock() == null || VersionUtils.checkOffHand(event.getHand())) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null || VersionUtils.getItemInHand(event.getPlayer()).getType() != Material.FLINT_AND_STEEL) {
      return;
    }
    if(event.getClickedBlock().getType() == Material.TNT) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
    if(event.getEntity().getType() != EntityType.PLAYER) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena((Player) event.getEntity());
    if(arena != null) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onDamage(EntityDamageEvent event) {
    if(event.getEntity().getType() != EntityType.PLAYER) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena((Player) event.getEntity());
    if(arena == null || arena.getArenaState() != ArenaState.IN_GAME) {
      return;
    }
    event.setCancelled(true);
    Player player = (Player) event.getEntity();
    if(player.getLocation().getY() < 1) {
      Plot plot = arena.getPlotManager().getPlot(player);
      if(plot != null) {
        player.teleport(plot.getTeleportLocation());
      }
    }
  }

  @EventHandler
  public void onTreeGrow(StructureGrowEvent event) {
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    Plot buildPlot = arena.getPlotManager().getPlot(event.getPlayer());
    if(buildPlot == null) {
      return;
    }
    for(BlockState blockState : event.getBlocks()) {
      if(!buildPlot.getCuboid().isIn(blockState.getLocation())) {
        blockState.setType(Material.AIR);
      }
    }
  }


  @EventHandler
  public void onDispense(BlockDispenseEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        if(buildPlot.getCuboid() != null && !buildPlot.getCuboid().isInWithMarge(event.getBlock().getLocation(), -1) && buildPlot.getCuboid().isInWithMarge(event.getBlock().getLocation(), 5)) {
          event.setCancelled(true);
        }
      }
    }
  }

  @Deprecated
  //only a temporary code
  @EventHandler
  public void onPlayerHeadsClick(InventoryClickEvent event) {
    if(!ItemUtils.isItemStackNamed(event.getCurrentItem()) || !(event.getWhoClicked() instanceof Player)) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena((Player) event.getWhoClicked());
    if(arena == null) {
      return;
    }
    if(plugin.getOptionsRegistry().getPlayerHeadsRegistry().isHeadsMenu(event.getInventory())) {
      if(event.getCurrentItem().getType() != ItemUtils.PLAYER_HEAD_ITEM.getType()) {
        return;
      }
      event.getWhoClicked().getInventory().addItem(event.getCurrentItem().clone());
      event.setCancelled(true);
    }
  }

  @Deprecated
  @EventHandler
  public void onOptionItemClick(InventoryClickEvent event) {
    if(!(event.getWhoClicked() instanceof Player) || !ItemUtils.isItemStackNamed(event.getCurrentItem())) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena((Player) event.getWhoClicked());
    String key = plugin.getSpecialItemsManager().getRelatedSpecialItem(event.getCurrentItem()).getName();
    if(key == null || arena == null) {
      return;
    }
    if(!key.equals(SpecialItemsManager.SpecialItems.OPTIONS_MENU.getName())) {
      return;
    }
    event.setResult(Event.Result.DENY);
    event.setCancelled(true);
  }

  @EventHandler
  public void onOptionItemClick(InventoryInteractEvent event) {
    if(!(event.getWhoClicked() instanceof Player) || !ItemUtils.isItemStackNamed(event.getWhoClicked().getItemOnCursor())) {
      return;
    }
    BaseArena arena = ArenaRegistry.getArena((Player) event.getWhoClicked());
    String key = plugin.getSpecialItemsManager().getRelatedSpecialItem(event.getWhoClicked().getItemOnCursor()).getName();
    if(key == null || arena == null) {
      return;
    }
    if(!key.equals(SpecialItemsManager.SpecialItems.OPTIONS_MENU.getName())) {
      return;
    }
    event.setResult(Event.Result.DENY);
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPreCommand(PlayerCommandPreprocessEvent event) {
    if(ArenaRegistry.getArena(event.getPlayer()) == null) {
      return;
    }
    if(!plugin.getConfigPreferences().getOption(ConfigPreferences.Option.BLOCK_COMMANDS_IN_GAME)) {
      return;
    }
    String command = event.getMessage().substring(1);
    int indexOf = command.indexOf(' ');
    command = (indexOf >= 0 ? command.substring(0, indexOf) : command);
    for(String string : plugin.getConfigPreferences().getWhitelistedCommands()) {
      if(command.equalsIgnoreCase(string)) {
        return;
      }
    }
    if(event.getPlayer().isOp() || event.getPlayer().hasPermission("buildbattle.admin") || event.getPlayer().hasPermission("buildbattle.command.bypass")) {
      return;
    }
    if(command.equalsIgnoreCase("bb") || command.equalsIgnoreCase("buildbattle") || command.equalsIgnoreCase("bba") ||
        command.equalsIgnoreCase("buildbattleadmin")) {
      return;
    }
    event.setCancelled(true);
    event.getPlayer().sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("In-Game.Only-Command-Ingame-Is-Leave"));
  }

  @EventHandler
  public void playerCommandExecution(PlayerCommandPreprocessEvent event) {
    if(plugin.getConfigPreferences().getOption(ConfigPreferences.Option.ENABLE_SHORT_COMMANDS)) {
      Player player = event.getPlayer();
      if(event.getMessage().equalsIgnoreCase("/start")) {
        player.performCommand("bba forcestart");
        event.setCancelled(true);
        return;
      }
      if(event.getMessage().equalsIgnoreCase("/leave")) {
        player.performCommand("bb leave");
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onBucketEmpty(PlayerBucketEmptyEvent event) {
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    Plot buildPlot = arena.getPlotManager().getPlot(event.getPlayer());
    if(buildPlot != null && buildPlot.getCuboid() != null && !buildPlot.getCuboid().isIn(event.getBlockClicked().getRelative(event.getBlockFace()).getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onBlockSpread(BlockSpreadEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      if(!arena.getPlotManager().getPlots().isEmpty()) {
        Plot plot = arena.getPlotManager().getPlots().get(0);
        if(plot != null && plot.getCuboid() != null
            && event.getBlock().getWorld().equals(plot.getCuboid().getCenter().getWorld()) && event.getSource().getType() == Material.FIRE) {
          event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCreatureSpawn(CreatureSpawnEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      if(arena.getPlotManager().getPlots().isEmpty() || arena.getPlotManager().getPlots().get(0) == null
          || !event.getEntity().getWorld().equals(arena.getPlotManager().getPlots().get(0).getCuboid().getCenter().getWorld())) {
        continue;
      }
      if(event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
        return;
      }
      if(event.getEntity().getType() == EntityType.WITHER || plugin.getConfig().getBoolean("Disable-Mob-Spawning-Completely", true)) {
        event.setCancelled(true);
        return;
      }
      for(Plot plot : arena.getPlotManager().getPlots()) {
        if(plot.getCuboid().isInWithMarge(event.getEntity().getLocation(), 1)) {
          if(plot.getEntities() >= plugin.getConfig().getInt("Mobs-Max-Amount-Per-Plot", 20)) {
            //todo maybe only for spawner player?
            for(Player p : plot.getMembers()) {
              p.sendMessage(plugin.getChatManager().colorMessage("In-Game.Max-Entities-Limit-Reached"));
            }
            event.setCancelled(true);
            return;
          }

          for(String entityNames : plugin.getConfig().getStringList("Restricted-Entities-Spawn")) {
            if(event.getEntity().getType().name().equalsIgnoreCase(entityNames)) {
              event.setCancelled(true);
              return;
            }
          }

          plot.addEntity();
          event.setCancelled(false);
          event.getEntity().setAI(false);
        }
      }
    }
  }

  @EventHandler
  public void onLeavesDecay(LeavesDecayEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        if(buildPlot.getCuboid() != null && buildPlot.getCuboid().isInWithMarge(event.getBlock().getLocation(), 5)) {
          event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler
  public void onIgniteEvent(BlockIgniteEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        if(buildPlot.getCuboid() != null && buildPlot.getCuboid().isInWithMarge(event.getBlock().getLocation(), 5)) {
          event.setCancelled(true);
        }
      }
    }
  }

  @EventHandler
  public void onPistonRetractEvent(BlockPistonRetractEvent event) {
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        for(Block block : event.getBlocks()) {
          if(buildPlot.getCuboid() != null && !buildPlot.getCuboid().isInWithMarge(block.getLocation(), -1) && buildPlot.getCuboid().isIn(event.getBlock().getLocation())) {
            event.setCancelled(true);
          }
        }
      }
    }
  }

  @EventHandler
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    if(ArenaRegistry.getArena(event.getPlayer()) == null) {
      return;
    }
    ItemStack drop = event.getItemDrop().getItemStack();
    if(!ItemUtils.isItemStackNamed(drop)) {
      return;
    }
    if(ComplementAccessor.getComplement().getDisplayName(drop.getItemMeta()).equals(plugin.getChatManager().colorMessage("Menus.Option-Menu.Inventory-Name")) || plugin.getVoteItems().getPoints(drop) != 0) {
      event.setCancelled(true);
    }
  }


  @EventHandler(priority = EventPriority.HIGH)
  public void onBreak(BlockBreakEvent event) {
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    if(arena.getArenaState() != ArenaState.IN_GAME || (arena instanceof SoloArena && ((SoloArena) arena).isVoting())
        || plugin.getConfigPreferences().getItemBlacklist().contains(event.getBlock().getType())) {
      event.setCancelled(true);
      return;
    }
    User user = plugin.getUserManager().getUser(event.getPlayer());
    Plot buildPlot = user.getCurrentPlot();

    if(buildPlot != null && buildPlot.getCuboid() != null && buildPlot.getCuboid().isIn(event.getBlock().getLocation())) {
      user.addStat(StatsStorage.StatisticType.BLOCKS_BROKEN, 1);
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlace(BlockPlaceEvent event) {
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    if(arena.getArenaState() != ArenaState.IN_GAME || plugin.getConfigPreferences().getItemBlacklist().contains(event.getBlock().getType())
        || (arena instanceof SoloArena && ((SoloArena) arena).isVoting())) {
      event.setCancelled(true);
      return;
    }
    if(arena instanceof GuessTheBuildArena && !event.getPlayer().equals(((GuessTheBuildArena) arena).getCurrentBuilder())) {
      event.setCancelled(true);
      return;
    }
    User user = plugin.getUserManager().getUser(event.getPlayer());
    Plot buildPlot = user.getCurrentPlot();

    if(buildPlot != null && buildPlot.getCuboid() != null && buildPlot.getCuboid().isIn(event.getBlock().getLocation())) {
      user.addStat(StatsStorage.StatisticType.BLOCKS_PLACED, 1);
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    BaseArena arena = ArenaRegistry.getArena((Player) event.getWhoClicked());
    if(arena == null) {
      return;
    }
    if(arena.getArenaState() != ArenaState.IN_GAME) {
      event.setCancelled(true);
      return;
    }
    if(arena instanceof SoloArena && !((SoloArena) arena).isVoting()) {
      return;
    }
    if(arena instanceof GuessTheBuildArena && event.getWhoClicked().equals(((GuessTheBuildArena) arena).getCurrentBuilder())) {
      return;
    }
    event.setCancelled(true);
  }

  @EventHandler
  public void onNPCClick(CBPlayerInteractEntityEvent event) {
    if(VersionUtils.checkOffHand(event.getHand()) || VersionUtils.getItemInHand(event.getPlayer()).getType() == Material.AIR) {
      return;
    }

    if(plugin.getUserManager().getUser(event.getPlayer()).isSpectator()) {
      return;
    }

    if(event.getRightClicked() instanceof Villager && event.getRightClicked().getCustomName() != null && event.getRightClicked().getCustomName().equalsIgnoreCase(plugin.getChatManager().colorMessage("In-Game.NPC.Floor-Change-NPC-Name"))) {
      BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
      if(arena == null || arena.getArenaState() != ArenaState.IN_GAME) {
        return;
      }
      if(arena instanceof SoloArena && ((SoloArena) arena).isVoting()) {
        return;
      }
      Material material = VersionUtils.getItemInHand(event.getPlayer()).getType();
      if(material != XMaterial.WATER_BUCKET.parseMaterial() && material != XMaterial.LAVA_BUCKET.parseMaterial()
          && !(material.isBlock() && material.isSolid() && material.isOccluding())) {
        event.getPlayer().sendMessage(plugin.getChatManager().colorMessage("In-Game.Floor-Item-Blacklisted"));
        return;
      }
      if(plugin.getConfigPreferences().getFloorBlacklist().contains(material)) {
        event.getPlayer().sendMessage(plugin.getChatManager().colorMessage("In-Game.Floor-Item-Blacklisted"));
        return;
      }
      arena.getPlotManager().getPlot(event.getPlayer()).changeFloor(material, VersionUtils.getItemInHand(event.getPlayer()).getData().getData());
      event.getPlayer().sendMessage(plugin.getChatManager().getPrefix() + plugin.getChatManager().colorMessage("Menus.Option-Menu.Items.Floor.Floor-Changed"));
    }
  }

  @EventHandler
  public void onEnderchestClick(CBPlayerInteractEvent event) {
    BaseArena arena = ArenaRegistry.getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    if(arena.getArenaState() != ArenaState.IN_GAME) {
      event.setCancelled(true);
      return;
    }
    Block block = event.getClickedBlock();
    if(block != null && block.getType() == XMaterial.ENDER_CHEST.parseMaterial()) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onMinecartMove(VehicleMoveEvent event) {
    Vehicle vehicle = event.getVehicle();
    if(!(vehicle instanceof Minecart)) {
      return;
    }
    for(BaseArena arena : ArenaRegistry.getArenas()) {
      for(Plot buildPlot : arena.getPlotManager().getPlots()) {
        if(buildPlot.getCuboid() != null && !buildPlot.getCuboid().isInWithMarge(event.getTo(), -1) && buildPlot.getCuboid().isIn(event.getTo())) {
          ((Minecart) vehicle).setMaxSpeed(0);
          vehicle.setVelocity(vehicle.getVelocity().zero());
        }
      }
    }
  }

  @EventHandler
  public void onItemFrameRotate(PlayerInteractEntityEvent event) {
    if(event.getRightClicked() instanceof ItemFrame && !((ItemFrame) event.getRightClicked()).getItem().getType().equals(Material.AIR)) {
      for(BaseArena arena : ArenaRegistry.getArenas()) {
        for(Plot buildPlot : arena.getPlotManager().getPlots()) {
          if(buildPlot.getCuboid() != null && !buildPlot.getCuboid().isInWithMarge(event.getRightClicked().getLocation(), -1) && buildPlot.getCuboid().isIn(event.getRightClicked().getLocation())) {
            event.setCancelled(true);
          }
        }
      }
    }
  }
}
