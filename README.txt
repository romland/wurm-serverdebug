2022 note: I posted original announcement here: https://forum.wurmonline.com/index.php?/topic/164491-released-friyas-server-debug-locating-bugs-and-performance-issues/

=====

 Intermittent crashes or freezes are horrible, using a "game hoster" or not. Servers have died and been shut down for less. This will at at the very least help pinning down possible problems that may be introduced by a mod, a combination of mods or just plain old server bugs.

 

I wrote this software to assist me in tracking down problems on a server that was getting intermittent freezes -- and it was rare enough that you want to be able to nail it when it first happens -- alas, I was not clairvoyant enough -- so here we have this mod -- a tad bigger than I thought it would become. I share it because I know I will rely on it in the future.

 

It's a bit of a mish-mash of things (*), but I left it all in there as configurable options.  It grew out of me eliminating causes of a problem. It was a server where I did not have any access, issue was not reproducible on a test server, and had an absolute crap-ton of mods. Read: I like a challenge (har har). Later on it it also grew into a tool to help me optimize the server code (or at least my behaviour around it). I have to say it's quite pleasant to be able to pinpoint and measure execution cost without modifying actual source code; I may actually start using a similar approach in non-hobby projects. :)

 

Dumps

If you have full control over your environment and have the technical abilities you may want to hook up some external tools to solve a lot of your problems. That said, this program can also output a stack and/or thread dump if it detects slow calls in a monitored area. It may even be easier to use this than other tools. Note that in many cases this will only point you in the direction of the problem. It could still mean you have a lot of work to do from there (i.e. chasing calls down the chain with more monitoring).

 

The server is half-decent at tracking laggy calls itself (albeit a lot less granularity than what is possible using this), aaand provided the call actually finishes! The original usage of the monitor.* properties outlined in the config file below was to distinguish it from a complete freeze or something else (the problem was rare enough that I threw things on the wall to see what stuck). That said, you can monitor execution cost of any method on the server (to pinpoint strange hot areas) -- and attach possible interesting objects from that context ... to really nail it. To do this, use the monitor.* setting below. You can add as many of those as you may need. It's a pretty powerful tool and if I wasn't chasing a bug I'd start looking at a few spots I have taken notes of where I can optimize the actual WU server (optimizing -is- fun).

 

Hard to reproduce exceptions

Funnily enough, it turns out this is a great way to pin down intermittent exceptions. If you add a monitor to a call that takes place in a stack trace -- and wait (without much pain), if that call never finishes (i.e. it threw an exception), a warning will be printed in the log (options to include traces as well). To get the detail of which object is the offending one (say a specific creature), add an argument to the monitor (e.g. $0 for this). For a few special cases (like Zone), instead of just grabbing toString() of the object I output e.g. zone X/Y. I intend to add more humanly readable strings of object as I need them, but yeah, it's primarily toString() at the moment. Combine arguments to a monitor with a full thread dump on a slow call and any freezing or hanging problem should become evident rather quickly. Albeit rather spammy if you let it hang too long. :)

 

A few warnings

    Monitoring methods will likely not play well with recursive methods, monitor those from the "outside" (never tried it)
    Monitoring methods that might be called by several threads simultaneously will get dodgy, no doubt (never tried it).

 

Performance impact

This mod is written with performance in mind, even if you choose to monitor tight loops it should not affect performance *too* much. The idea is that you can have this running on a live server and just wait for the problem to happen. That said, be aware that it does add a bit of overhead. And might be noticeable in hot tight loops. The cost of having something nice... :) 

 

Searching in spammy logs

In order to find warnings in very spammy logs, search for "Hey Friya" (I kept it in there because I could) -- any offender will be tagged with those words. If you find too many "Hey Friya"'s, it probably means your slow call thresholds are too low -- set them higher.

 

(*) the mish-mash:

    Dump methods calls in a method (in a format for easy copy/pasting for monitoring.* ;)
    Log a brief version of data stream coming to and/or going from the server
    Logging of all actions created (read: new Action()), whether by NPC's or Players
    Slow mod detection; monitoring default mod hooks
    Monitor if main loop is still ticking
    Dump all classes modified by Javassist (I do not decompile them for you :P)
    Dump thread info on slow running calls
    Monitor itself (! okay, this is a lie, but it will output a message every now and then to indicate this)
    A few more pointless monitoring options (they all sit in the same thread -- hence pointless)
    Storage of monitored methods executed since entering a hooked method (this helps determine the code path taken through a method)
    Various options to specify frequency at which things can happen
    Dump stack-trace on completion of a slow call
    Dump stack-trace of a monitored call that is still running
    ... and a few more things.

 

Well, outta here. Cya on the flip side. 


Example config:

#
# Friya's Server Debugging (and perf meter)
# """""""""""""""""""""""""""""""""""""""""
#
# See Wurm Online Forums.
#
#             -- Friya, 2018
#                http://www.filterbubbles.com (for some of my other projects)
#

# Leave these three as-is.
classname=com.filterbubbles.wu.serverdebug.Mod
classpath=serverdebug.jar
sharedClassLoader=true

#
# Set to false to disable all functionality of this software.
#
# Default:	true
#
enabled=true

#
# Set to a class and method and I'll dump all method calls from said 
# method. It will then throw an exception and halt the starting up 
# at the earliest possible opportunity.
#
# This is merely for convenience but comes in very useful for locating
# which calls you want to monitor using the monitor.* options below.
#
# If you want to start your server, leave empty or comment out. :)
#
#listCallsInMethodThenExit=com.wurmonline.server.zones.Zones.pollNextZones
#listCallsInMethodThenExit=com.wurmonline.server.creatures.Creatures.pollAllCreatures
#listCallsInMethodThenExit=com/wurmonline/server/creatures/Creature.poll
#listCallsInMethodThenExit=com/wurmonline/server/creatures/Creature.checkMove
#listCallsInMethodThenExit=com/wurmonline/server/creatures/Creature.takeSimpleStep

#
# Log data streams coming in from client (spammy)
#
# Default:	false
#
logCmdIn=false

#
# Log data streams going to client (spammy)
#
# Default:	false
#
logCmdOut=false

#
# Log (attempted) new actions (by both NPC's and players)
#
# Default:	true
#
logNewAction=true

#
# Monitor hooks provided by Ago's modlauncher, if something takes too long to
# execute this mod will warn about it and its location. This is monitored from a 
# separate thread, which means if server hangs it will still give some sensible 
# output.
#
# Default:	true
#
monitorDefaultModHooks=true

#
# Add a lot of monitoring of the server's mainloop (Server.run()). Same as 
# monitorDefaultModHooks but in Wurm proper.
#
# Default:	true
#
monitorGameLoop=true

#
# Output all classes that were modified with Javassist (with their modifications).
# They will end up in ..\WurmServerLauncher\mods\serverdebug\dump\ ...
#
# Default:	false
#
dumpModifiedClasses=false

#
# This will dump information on all currently running threads. This will be spammy
# if you have a lot of slow calls. Set the monitoring thresholds a bit higher if 
# you are looking for a complete freeze or severe lag. 
#
# Consider combining this with arguments in monitoring calls so that you can find
# out which objects may be the offending one -- and then go from there.
# 
# Default:	false (because it's spammy, but do not hesitate to set to true)
#
dumpThreadInfoOnSlowRunningCall=true

#
# Periodic output stating that the monitoring is still running. This one you 
# probably want to have as true as there might be bugs in this mod that makes 
# the monitoring thread stop; you want to know if that happens.
#
# Default:	true
#
monitorHealthNotice=true

#
# Periodic output that Ago's Modlauncher's server polling is still running. 
# Technically you only need one of: serverPollHealthNotice, loginHandlerHealthNotice
# and socketServerHealthNotice enabled as they all run in the same thread.
#
# Default:	false
#
serverPollHealthNotice=true

#
# See serverPollHealthNotice.
#
# Default:	false
#
loginHandlerHealthNotice=true

#
# See serverPollHealthNotice.
#
# Default:	false
#
socketServerHealthNotice=true

#
# A frame begins as a hooked method is entered. From this point, monitored 
# calls made from the mothod are temporarily stored. This will help you determine
# where and how a method called something when a thread is hanging or lagging.
#
# The frame is cleared on every entry (not at exit).
#
# This sets the maximum size of the call-frame-buffer, setting this too large
# might consume a lot of memory if you are monitoring calls in a tight loop.
#
# This maximum is applied per hooked method. Set it to 0 to disable the 
# functionality.
#
# ALTERNATIVE EXPLANATION (FRIYA TODO, DECIDE WHICH TO USE)
# If a slow call is encountered, output which calls were made this "frame" 
# from the hooked method. This is handy if you are not showing stack trace
# OR if you need to figure out which code-path was run prior to the monitored
# call. To disable this, set to 0.
#
# Default:	100
#
maxStoredFrameCalls=100

#
# When is a call deemed as taking too long (milliseconds). 
# This will be used to determine whether it was a long call -after- it was 
# executed. Note that this will never get a chance to be used if a call 
# never finishes. Use monitorTickWarningThreshold for that.
#
# Default:	1000 (1 second)
#
defaultLongCallThreshold=1000

#
# How often should the monitor check the game and/or modlauncher for problems,
# this is in milliseconds.
#
# Default:	5000 (5 seconds)
#
monitorTickFrequency=5000

#
# How long a call must have been running before monitor tick will start 
# outputting warnings about it. For as long as the offending call is being
# executed the warning will be printed every five seconds (or whatever the 
# length of monitorTickFrequency is currently set to).
#
monitorTickWarningThreshold=3000

#
# Will output a stack trace if a call that was completed was above set 
# threshold. The trace ends at the monitored call.
#
# Default:	true
#
completionStackTrace=true

#
# Will output a stack trace of slow calls that are still running. Note that
# this will consume more memory as the stack trace gets temporarily stored 
# (increases with number of hooked methods - NOT calls) when the call begins.
# This also means the trace ends at the monitored call so to trace a path
# through the hooked method, you'd also want to enable the call frame storing.
#
# Default:	false
#
runningStackTrace=true

#
# Attach 'this' to a monitor (unless hooked method is static) if there is no 
# other argument requested in a monitored call.
#
# The trace is taken as a hooked method is entered, not on every call. Which 
# means the performance penalty is not -that- severe if the call itself lives
# in a tight loop.
#
# Default:	false
#
attachThisToMonitors=false

#
# monitor [calls from] <name of class> and <name of method> to class.method(s).
#
# The following example will monitor every large suspect in the mainloop, which is 
# probably a good start for any bug-hunt. That is, com.wurmonline.server.Server 
# and run() method.
#
# You can output an object coming from the source by doing e.g.:
#	com.wurmonline.server.zones.Zones.pollNextZones($1) -- Javassist rules apply for $x
#	or com.wurmonline.server.zones.Zones.pollNextZones($0) for object being called
#	or com.wurmonline.server.zones.Zones.pollNextZones(nameOfVariable) for a declared 
#	variable in calling class, etc, etc.
#
# In case you didn't know backslash (\) escapes new line in Properties (as seen below).
#
# prefix | name of the hooked class | name of hooked method | class of a call (and method) in the hooked class/method to monitor
#  \/             \/                  \/                          \/              \/
monitor.com.wurmonline.server.Server.run=com.wurmonline.server.zones.TilePoller.pollNext,\
	com.wurmonline.server.zones.Zones.pollNextZones,\
	com.wurmonline.server.zones.Zones.pollNextZones,\
	com.wurmonline.server.zones.CropTilePoller.pollCropTiles,\
	com.wurmonline.server.Players.pollPlayers,\
	com.wurmonline.server.creatures.Delivery.poll,\
	com.wurmonline.server.support.VoteQuestions.handleVoting,\
	com.wurmonline.server.support.VoteQuestions.handleArchiveTickets,\
	com.wurmonline.server.highways.Routes.handlePathsToSend,\
	com.wurmonline.server.players.PlayerInfoFactory.handlePlayerStateList,\
	com.wurmonline.server.support.Tickets.handleArchiveTickets,\
	com.wurmonline.server.support.Tickets.handleTicketsToSend,\
	com.wurmonline.server.WurmCalendar.tickSecond,\
	com.wurmonline.server.combat.ServerProjectile.pollAll,\
	com.wurmonline.server.players.PlayerInfoFactory.switchFatigue,\
	com.wurmonline.server.creatures.Offspring.resetOffspringCounters,\
	com.wurmonline.server.kingdom.King.pollKings,\
	com.wurmonline.server.Players.checkElectors,\
	com.wurmonline.server.combat.Arrows.pollAll,\
	com.wurmonline.server.epic.Hota.poll,\
	com.wurmonline.server.items.WurmMail.poll,\
	com.wurmonline.server.structures.Fence.poll,\
	com.wurmonline.server.structures.Wall.poll,\
	com.wurmonline.server.Players.removeGlobalEffect,\
	com.wurmonline.server.Server.pollSurfaceWater,\
	com.wurmonline.server.weather.Weather.tick,\
	com.wurmonline.server.Server.startSendWeatherThread,\
	com.wurmonline.server.zones.Zones.loadChristmas,\
	com.wurmonline.server.zones.Zones.deleteChristmas,\
	com.wurmonline.server.zones.Zones.flash,\
	com.wurmonline.server.skills.SkillStat.pollSkills,\
	com.wurmonline.server.endgames.EndGameItems.pollAll,\
	com.wurmonline.server.zones.Trap.checkUpdate,\
	com.wurmonline.server.Items.pollUnstableRifts,\
	com.wurmonline.server.zones.Dens.checkDens,\
	com.wurmonline.server.zones.Zones.saveProtectedTiles,\
	com.wurmonline.server.epic.HexMap.pollAllEntities,\
	com.wurmonline.server.villages.RecruitmentAds.poll,\
	com.wurmonline.server.epic.ValreiMapData.pollValreiData,\
	com.wurmonline.server.Server.pollPendingAwards,\
	com.wurmonline.server.deities.Deities.calculateFaiths,\
	com.wurmonline.server.Players.resetFaithGain,\
	com.wurmonline.server.creatures.Creatures.pollOfflineCreatures,\
	com.wurmonline.server.Server.addIntraCommand,\
	com.wurmonline.server.zones.ErrorChecks.checkItemWatchers,\
	com.wurmonline.server.players.PendingAccount.poll,\
	com.wurmonline.server.Items.countEggs,\
	com.wurmonline.server.banks.Banks.poll,\
	com.wurmonline.server.Players.checkAffinities,\
	com.wurmonline.server.Players.calcCRBonus,\
	com.wurmonline.server.villages.Villages.poll,\
	com.wurmonline.server.kingdom.Kingdoms.poll,\
	com.wurmonline.server.questions.Questions.trimQuestions,\
	com.wurmonline.server.skills.Skills.switchSkills,\
	com.wurmonline.server.combat.Battles.poll,\
	com.wurmonline.server.ServerEntry.saveTimers,\
	com.wurmonline.server.Players.pollChamps,\
	com.wurmonline.server.epic.Effectuator.pollEpicEffects,\
	com.wurmonline.server.players.PlayerInfoFactory.checkIfDeleteOnePlayer,\
	com.wurmonline.server.players.PlayerInfoFactory.pruneRanks,\
	com.wurmonline.server.epic.EpicServerStatus.pollExpiredMissions,\
	com.wurmonline.server.Server.pollTerraformingTasks,\
	com.wurmonline.server.items.Item.checkItemSpawn,\
	com.wurmonline.server.Items.getWarTargets,\
	com.wurmonline.server.kingdom.Kingdom.addWinpoints,\
	com.wurmonline.server.statistics.ChallengeSummary.saveCurrentGlobalHtmlPage,\
	com.wurmonline.server.zones.Trap.checkQuickUpdate,\
	com.wurmonline.server.Players.checkSendWeather,\
	com.wurmonline.server.Players.logOffLinklessPlayers,\
	com.wurmonline.server.Server.checkAlertMessages,\
	com.wurmonline.server.players.Cultist.resetSkillGain,\
	com.wurmonline.server.Server.pollShopDemands,\
	com.wurmonline.server.zones.AreaSpellEffect.pollEffects,\
	com.wurmonline.server.Players.printStats,\
	com.wurmonline.server.behaviours.Methods.resetAspirants,\
	com.wurmonline.server.creatures.Creature.createVisionArea,\
	com.wurmonline.server.Server.removeCreatures,\
	com.wurmonline.server.Server.pollWebCommands,\
	com.wurmonline.server.MessageServer.sendMessages,\
	com.wurmonline.server.Server.sendFinals($0),\
	com.wurmonline.server.Server.pollComms,\
	com.wurmonline.server.Server.pollIntraCommands,\
	com.wurmonline.server.steam.SteamHandler.update

#
# Specifies the threshold which we'll use to indicate when a call is slow.
# This will override the "defaultLongCallThreshold" setting for calls in 
# this method; com.wurmonline.server.Server.run().
#
# If left unspecified, the "defaultLongCallThreshold" is used.
#
monitorThreshold.com.wurmonline.server.Server.run=1000

#
# This adds a monitor of the call to pollAllCreatures() from the method 
# pollNextZones() in the Zones class.
#
# Additionally it sets the threshold to warn if this call's duration 
# exceed 50 milliseconds. The warning will appear in the server log,
# just search for "Hey Friya".
#
# See above for more detailed information.
#
monitor.com.wurmonline.server.zones.Zones.pollNextZones=\
	com.wurmonline.server.creatures.Creatures.pollAllCreatures($1),\
	com.wurmonline.server.players.PlayerInfoFactory.pollPremiumPlayers,\
	com.wurmonline.server.zones.FocusZone.pollAll,\
	com.wurmonline.server.creatures.CombatHandler.resolveRound,\
	com.wurmonline.server.Players.pollDeadPlayers,\
	com.wurmonline.server.Players.pollKosWarnings,\
	com.wurmonline.server.zones.Zone.poll($0)
# See above.
monitorThreshold.com.wurmonline.server.zones.Zones.pollNextZones=1000

# See above.
monitor.com.wurmonline.server.creatures.Creatures.pollAllCreatures=\
	com.wurmonline.server.creatures.Creatures.getCreatures,\
	com.wurmonline.server.Players.getPlayers,\
	com.wurmonline.server.players.Player.getCurrentTile($0),\
	com.wurmonline.server.players.Player.poll($0),\
	com.wurmonline.server.zones.VolaTile.deleteCreature($1),\
	com.wurmonline.server.players.Player.doLavaDamage,\
	com.wurmonline.server.zones.VolaTile.doAreaDamage,\
	com.wurmonline.server.creatures.Creature.poll($0)
# See above.
monitorThreshold.com.wurmonline.server.creatures.Creatures.pollAllCreatures=1000

# See above.
monitor.com.wurmonline.server.creatures.Creature.poll=\
	com.wurmonline.server.creatures.ai.CreatureAI.pollCreature,\
	com.wurmonline.server.creatures.Creature.checkBreedCounter,\
	com.wurmonline.server.creatures.Creature.getInventory,\
	com.wurmonline.server.items.Item.pollCoolingItems,\
	com.wurmonline.server.creatures.CreatureStatus.getPath,\
	com.wurmonline.server.creatures.Creature.die,\
	com.wurmonline.server.creatures.Creature.handleCreatureOutOfBounds,\
	com.wurmonline.server.creatures.CreatureStatus.pollDetectInvis,\
	com.wurmonline.server.creatures.SpellEffects.poll,\
	com.wurmonline.server.creatures.Creature.pollNPCChat,\
	com.wurmonline.server.behaviours.ActionStack.poll($1),\
	com.wurmonline.server.creatures.Creature.attackTarget,\
	com.wurmonline.server.creatures.Creature.findFood,\
	com.wurmonline.server.creatures.Creature.pollNPC,\
	com.wurmonline.server.creatures.Creature.checkEggLaying,\
	com.wurmonline.server.creatures.Creature.pollAge,\
	com.wurmonline.server.creatures.Creature.checkMove($0),\
	com.wurmonline.server.creatures.Creature.startUsingPath,\
	com.wurmonline.server.creatures.CreatureStatus.pollFat,\
	com.wurmonline.server.creatures.Creature.checkForEnemies,\
	com.wurmonline.server.creatures.MovementScheme.setWebArmourMod,\
	com.wurmonline.server.epic.EpicServerStatus.doesGiveItemMissionExist,\
	com.wurmonline.server.creatures.Creature.pollItems,\
	com.wurmonline.server.creatures.Creature.sendItemsTaken,\
	com.wurmonline.server.creatures.Creature.sendItemsDropped,\
	com.wurmonline.server.creatures.Creature.pollStamina,\
	com.wurmonline.server.creatures.Creature.pollFavor,\
	com.wurmonline.server.creatures.Creature.pollLoyalty,\
	com.wurmonline.server.creatures.Creatures.setCreatureOffline,\
	com.wurmonline.server.creatures.Creature.savePosition
# See above. (NOTE TO MYSELF: Set this to 1 -- some things looked hilariously expensive here)
monitorThreshold.com.wurmonline.server.creatures.Creature.poll=500

# See above.
monitor.com.wurmonline.server.creatures.Creature.checkMove=\
	com.wurmonline.server.creatures.Creature.isSentinel,\
	com.wurmonline.server.creatures.Creature.isHorse,\
	com.wurmonline.server.creatures.Creature.isUnicorn,\
	com.wurmonline.server.creatures.Creature.getWornItem,\
	com.wurmonline.server.items.Item.isSaddleLarge,\
	com.wurmonline.server.items.Item.isSaddleNormal,\
	com.wurmonline.server.creatures.Creature.isDominated,\
	com.wurmonline.server.creatures.Creature.hasOrders,\
	com.wurmonline.server.creatures.CreatureStatus.getPath,\
	com.wurmonline.server.creatures.Creature.startPathing,\
	com.wurmonline.server.creatures.Creature.moveAlongPath,\
	com.wurmonline.server.creatures.CreatureStatus.setPath,\
	com.wurmonline.server.creatures.Creature.turnTowardsCreature,\
	com.wurmonline.server.creatures.Creatures.getNpcs,\
	com.wurmonline.server.creatures.Npc.isWithinDistanceTo,\
	com.wurmonline.server.creatures.Creature.hunt,\
	com.wurmonline.server.creatures.Creature.startPathingToTile,\
	com.wurmonline.server.zones.VolaTile.getAllFences,\
	com.wurmonline.server.creatures.Creature.turnTowardsTile,\
	com.wurmonline.server.creatures.CreatureStatus.isUnconscious,\
	com.wurmonline.server.creatures.CreatureStatus.getStunned,\
	com.wurmonline.server.creatures.Creature.isMoveGlobal,\
	com.wurmonline.server.creatures.Creature.isTeleporting,\
	com.wurmonline.server.creatures.CreatureStatus.setMoving,\
	com.wurmonline.server.creatures.Creature.isHunter,\
	com.wurmonline.server.creatures.Creature.hunt,\
	com.wurmonline.server.creatures.Creature.getPersonalTargetTile,\
	com.wurmonline.server.creatures.Creature.isCareful,\
	com.wurmonline.server.creatures.Creature.getStatus,\
	com.wurmonline.server.creatures.Creature.isBred,\
	com.wurmonline.server.creatures.Creature.isBranded,\
	com.wurmonline.server.creatures.Creature.isCaredFor,\
	com.wurmonline.server.creatures.Creature.isNpc,\
	com.wurmonline.server.creatures.Creature.isAggHuman,\
	com.wurmonline.server.creatures.Creature.getCitizenVillage,\
	com.wurmonline.server.creatures.Creature.getWurmId,\
	com.wurmonline.server.creatures.CreatureTemplate.getMoveRate,\
	com.wurmonline.server.creatures.Creature.shouldFlee,\
	com.wurmonline.server.Features$Feature.isEnabled,\
	com.wurmonline.server.creatures.CreatureTemplate.getMoveRate,\
	com.wurmonline.server.creatures.Creature.shouldFlee,\
	com.wurmonline.server.creatures.Creature.getPathfindCounter,\
	com.wurmonline.server.creatures.ai.PathTile.getTileX,\
	com.wurmonline.server.creatures.ai.PathTile.getTileY,\
	com.wurmonline.server.creatures.Creature.getPathfindCounter,\
	com.wurmonline.server.creatures.Creature.turnTowardsTile,\
	com.wurmonline.server.creatures.Creature.takeSimpleStep,\
	com.wurmonline.server.creatures.CreatureStatus.setMoving,\
	com.wurmonline.server.creatures.Creature.moveAlongPath,\
	com.wurmonline.server.creatures.CreatureStatus.setPath,\
	com.wurmonline.server.creatures.CreatureStatus.setMoving
# See above.
monitorThreshold.com.wurmonline.server.creatures.Creature.checkMove=500

# See above
monitor.com.wurmonline.server.creatures.Creature.takeSimpleStep=\
	com.wurmonline.server.creatures.Creature.getSize,\
	com.wurmonline.server.creatures.Creature.isTargetTileTooHigh,\
	com.wurmonline.server.creatures.Creature.getLowestTileCorner,\
	com.wurmonline.server.creatures.Creature.turnTowardsTile,\
	com.wurmonline.server.creatures.Creature.rotateRandom,\
	com.wurmonline.server.creatures.Creature.normalizeAngle,\
	com.wurmonline.server.creatures.Creature.getTarget,\
	com.wurmonline.server.creatures.Creature.turnTowardsCreature,\
	com.wurmonline.server.creatures.Creature.getMoveModifier,\
	com.wurmonline.server.creatures.Creature.getSpeed,\
	com.wurmonline.server.creatures.Creature.isGhost,\
	com.wurmonline.mesh.MeshIO.getTile,\
	com.wurmonline.server.creatures.Creature.getName,\
	com.wurmonline.server.creatures.Creature.die,\
	com.wurmonline.mesh.MeshIO.getTile,\
	com.wurmonline.mesh.Tiles.decodeType,\
	com.wurmonline.mesh.Tiles.isSolidCave,\
	com.wurmonline.server.creatures.MineDoorPermission.getPermission,\
	com.wurmonline.server.creatures.MineDoorPermission.mayPass,\
	com.wurmonline.server.creatures.Creature.setLayer,\
	com.wurmonline.server.creatures.Creature.getBridgeId,\
	com.wurmonline.server.creatures.Creature.followsGround,\
	com.wurmonline.server.structures.Blocking.getBlockerBetween,\
	com.wurmonline.server.structures.Blocking.getBlockerBetween,\
	com.wurmonline.server.structures.BlockingResult.getFirstBlocker,\
	com.wurmonline.server.creatures.Creature.isKingdomGuard,\
	com.wurmonline.server.creatures.Creature.isSpiritGuard,\
	com.wurmonline.server.structures.Blocker.isDoor,\
	com.wurmonline.server.creatures.CreatureStatus.setMoving,\
	com.wurmonline.server.zones.VolaTile.isGuarded,\
	com.wurmonline.server.creatures.Creature.isAnimal,\
	com.wurmonline.server.zones.VolaTile.hasFire,\
	com.wurmonline.server.creatures.Creature.destroy,\
	com.wurmonline.server.creatures.Creature.getFloorLevel,\
	com.wurmonline.server.zones.Zones.calculateHeight,\
	com.wurmonline.server.creatures.Creature.isFloating,\
	com.wurmonline.server.creatures.Creature.isSubmerged,\
	com.wurmonline.server.creatures.Creature.setTarget,\
	com.wurmonline.server.creatures.Creature.isSwimming,\
	com.wurmonline.server.creatures.Creature.moved
# see above
monitorThreshold.com.wurmonline.server.creatures.Creature.takeSimpleStep=100
