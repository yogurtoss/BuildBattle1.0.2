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

package plugily.projects.buildbattle.arena.impl;

import org.bukkit.entity.Player;
import plugily.projects.buildbattle.Main;
import plugily.projects.buildbattle.arena.managers.plots.Plot;
import plugily.projects.buildbattle.arena.options.ArenaOption;
import plugily.projects.buildbattle.utils.Debugger;

import java.util.Comparator;

/**
 * @author Plajer
 * <p>
 * Created at 11.01.2019
 */
public class TeamArena extends SoloArena {

  public TeamArena(String id, Main plugin) {
    super(id, plugin);
  }

  @Override
  public void setMinimumPlayers(int amount) {
    if(amount <= getPlotSize()) {
      Debugger.debug(Debugger.Level.WARN, "Minimum players amount for TEAM game mode arena cannot be less than 3! Setting amount to 3!");
      setOptionValue(ArenaOption.MINIMUM_PLAYERS, 3);
      return;
    }
    super.setMinimumPlayers(amount);
  }

  @Override
  public void distributePlots() {
    for(Player player : getPlayers()) {
      // get base with min players
      Plot minPlayers = getPlotManager().getPlots().stream().min(Comparator.comparing(Plot::getMembersSize)).get();
      // add player to min base if he got no base
      Plot playerPlot = getPlotManager().getPlot(player);
      if(playerPlot == null) {
        minPlayers.addMember(player, this, true);
      }
      // fallback
      if(playerPlot == null) {
        Plot firstPlot = getPlotManager().getPlots().get(0);
        firstPlot.addMember(player, this, true);
      }
    }
    //check if not only one plot got players
    Plot maxPlayers = getPlotManager().getPlots().stream().max(Comparator.comparing(Plot::getMembersSize)).get();
    Plot minPlayers = getPlotManager().getPlots().stream().min(Comparator.comparing(Plot::getMembersSize)).get();
    if(maxPlayers.getMembersSize() == getPlayers().size()) {
      for(int i = 0; i < maxPlayers.getMembersSize() / 2; i++) {
        Player move = maxPlayers.getMembers().get(i);
        minPlayers.addMember(move, this, true);
        maxPlayers.removeMember(move);
      }
    }
    getPlotManager().teleportToPlots();
    /*if (!players.isEmpty()) {
      MessageUtils.errorOccurred();
      Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[Build Battle] [PLOT WARNING] Not enough plots in arena " + getID() + "!");
      Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[PLOT WARNING] Required " + Math.ceil((double) getPlayers().size() / 2) + " but have " + getPlotManager().getPlots().size());
      Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[PLOT WARNING] Instance was stopped!");
      ArenaManager.stopGame(false, this);
    }*/
  }

  @Override
  public boolean enoughPlayersToContinue() {
    int size = getPlayers().size();

    if(size > getPlotSize()) {
      return true;
    }

    if(size == getPlotSize()) {
      return !getPlotManager().getPlot(getPlayers().get(0)).getMembers().containsAll(getPlayers());
    }

    return false;
  }

}
