package com.alphadraxonis.sandboxpixeldungeon.editor.levels;

import com.alphadraxonis.sandboxpixeldungeon.Assets;
import com.alphadraxonis.sandboxpixeldungeon.Challenges;
import com.alphadraxonis.sandboxpixeldungeon.Dungeon;
import com.alphadraxonis.sandboxpixeldungeon.SandboxPixelDungeon;
import com.alphadraxonis.sandboxpixeldungeon.actors.buffs.ChampionEnemy;
import com.alphadraxonis.sandboxpixeldungeon.actors.mobs.Mob;
import com.alphadraxonis.sandboxpixeldungeon.actors.mobs.npcs.NPC;
import com.alphadraxonis.sandboxpixeldungeon.editor.editcomps.parts.transitions.TransitionEditPart;
import com.alphadraxonis.sandboxpixeldungeon.editor.quests.GhostQuest;
import com.alphadraxonis.sandboxpixeldungeon.editor.quests.ImpQuest;
import com.alphadraxonis.sandboxpixeldungeon.editor.quests.QuestNPC;
import com.alphadraxonis.sandboxpixeldungeon.editor.quests.WandmakerQuest;
import com.alphadraxonis.sandboxpixeldungeon.editor.util.CustomDungeonSaves;
import com.alphadraxonis.sandboxpixeldungeon.items.Heap;
import com.alphadraxonis.sandboxpixeldungeon.items.Item;
import com.alphadraxonis.sandboxpixeldungeon.items.Torch;
import com.alphadraxonis.sandboxpixeldungeon.levels.CavesBossLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.CavesLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.CityBossLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.CityLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.DeadEndLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.HallsBossLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.HallsLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.LastLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.Level;
import com.alphadraxonis.sandboxpixeldungeon.levels.PrisonBossLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.PrisonLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.RegularLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.SewerBossLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.SewerLevel;
import com.alphadraxonis.sandboxpixeldungeon.levels.Terrain;
import com.alphadraxonis.sandboxpixeldungeon.levels.builders.Builder;
import com.alphadraxonis.sandboxpixeldungeon.levels.features.LevelTransition;
import com.alphadraxonis.sandboxpixeldungeon.levels.rooms.Room;
import com.alphadraxonis.sandboxpixeldungeon.levels.rooms.special.ShopRoom;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelScheme implements Bundlable, Comparable<LevelScheme> {

    public static final LevelScheme SURFACE_LEVEL_SCHEME = new LevelScheme();//Placeholder for selecting levels
    public static final LevelScheme NO_LEVEL_SCHEME = new LevelScheme();//Placeholder for selecting levels

    static {
        SURFACE_LEVEL_SCHEME.name = Level.SURFACE;
        NO_LEVEL_SCHEME.name = Level.NONE;
    }


    String name;
    CustomDungeon customDungeon;

    private int depth;
    private String chasm = null, passage;
    private LevelTransition entranceTransitionRegular, exitTransitionRegular;//only for type Regular

    private Class<? extends Level> type;
    private Level level;
    private int numInRegion = 3;
    private Level.Feeling feeling;
    private int shopPriceMultiplier = 1;

    private long seed;
    private boolean seedSet = false;
    public List<Integer> entranceCells, exitCells;

    public List<Mob> mobsToSpawn;
    public List<Room> roomsToSpawn;//TODO also choose builder
    public List<Item> itemsToSpawn, prizeItemsToSpawn;

    public boolean spawnStandartRooms = true, spawnSecretRooms = true, spawnSpecialRooms = true;
    public boolean spawnMobs = true, spawnItems = true;

    public Class<? extends Builder> builder;

    //Challenge stuff
    public boolean spawnTorchIfDarkness = true, reduceViewDistanceIfDarkness = true,
            affectedByNoScrolls = true, rollForChampionIfChampionChallenge = true;


    public LevelScheme() {
    }

    //Wrapper for creating templates
    LevelScheme(Level level, int numInRegion) {
        this.name = Level.NONE;
        this.level = level;
        this.numInRegion = numInRegion;

        mobsToSpawn = new ArrayList<>(4);
        roomsToSpawn = new ArrayList<>(4);
        itemsToSpawn = new ArrayList<>(4);
        prizeItemsToSpawn = new ArrayList<>(4);
    }

    public void initNewLevelScheme(String name, Class<? extends Level> levelTemplate) {
        this.name = name;
        exitCells = new ArrayList<>(3);
        entranceCells = new ArrayList<>(3);

        if (type == CustomLevel.class) {
            level = new CustomLevel(name, levelTemplate, feeling, seedSet ? seed : null, numInRegion, depth, this);
        } else {
            initExitEntranceCellsForRandomLevel();
        }
        shopPriceMultiplier = Dungeon.getSimulatedDepth(this) / 5 + 1;

    }

    //These setters are ONLY for NewFloorComp
    public void setType(Class<? extends Level> type) {
        this.type = type;
    }

    public void setNumInRegion(int numInRegion) {
        this.numInRegion = numInRegion;
    }

    //These setters/getters are ONLY for LevelGenComp
    public void resetSeed() {
        seed = 0;
        seedSet = false;
    }

    public boolean isSeedSet() {
        return seedSet;
    }

    public LevelScheme(String name, int depth, CustomDungeon customDungeon) {
        this.name = name;
        this.depth = depth;
        this.customDungeon = customDungeon;

        mobsToSpawn = new ArrayList<>(4);
        roomsToSpawn = new ArrayList<>(4);
        itemsToSpawn = new ArrayList<>(4);
        prizeItemsToSpawn = new ArrayList<>(4);
        if (depth < 26) setChasm(Integer.toString(depth + 1));

        switch (depth) {
            case 1:
            case 2:
            case 3:
            case 4:
                type = SewerLevel.class;
                break;
            case 5:
                type = SewerBossLevel.class;
                customDungeon.addRatKingLevel(name);
                break;
            case 6:
            case 7:
            case 8:
            case 9:
                type = PrisonLevel.class;
                break;
            case 10:
                type = PrisonBossLevel.class;
                break;
            case 11:
            case 12:
            case 13:
            case 14:
                type = CavesLevel.class;
                break;
            case 15:
                type = CavesBossLevel.class;
                break;
            case 16:
            case 17:
            case 18:
            case 19:
                type = CityLevel.class;
                break;
            case 20:
                type = CityBossLevel.class;
                break;
            case 21:
            case 22:
            case 23:
            case 24:
                type = HallsLevel.class;
                break;
            case 25:
                type = HallsBossLevel.class;
                break;
            case 26:
                type = LastLevel.class;
                break;
            default:
                type = DeadEndLevel.class;
        }
        numInRegion = (depth - 1) % 5 + 1;

        if (depth == 6 || depth == 11 || depth == 16) roomsToSpawn.add(new ShopRoom());

        shopPriceMultiplier = depth / 5 + 1;
        passage = Integer.toString(((depth - 1) / 5) * 5 + 1);

        exitCells = new ArrayList<>(3);
        entranceCells = new ArrayList<>(3);
        initExitEntranceCellsForRandomLevel();
        if (depth > 1) {
            entranceTransitionRegular.destLevel = Integer.toString(depth - 1);
        }
        exitTransitionRegular.destLevel = Integer.toString(depth + 1);
    }

    private void initExitEntranceCellsForRandomLevel() {
        if (type != DeadEndLevel.class && type != LastLevel.class)
            exitCells.add(TransitionEditPart.DEFAULT);
        else exitCells.add(TransitionEditPart.NONE);
        entranceCells.add(TransitionEditPart.DEFAULT);
        entranceTransitionRegular = new LevelTransition(Level.SURFACE, -1);
        exitTransitionRegular = new LevelTransition(null, -1);
    }

    public String getName() {
        return name;
    }

    public int getDepth() {
        return depth;
    }

    public String getChasm() {
        return chasm;
    }

    public String getDefaultAbove() {
        if (entranceTransitionRegular == null) return Level.SURFACE;
        return entranceTransitionRegular.destLevel;
    }

    public String getDefaultBelow() {
        if (exitTransitionRegular == null) return null;
        return exitTransitionRegular.destLevel;
    }

    public LevelTransition getEntranceTransitionRegular() {
        return entranceTransitionRegular;
    }

    public LevelTransition getExitTransitionRegular() {
        return exitTransitionRegular;
    }

    public String getPassage() {//Passage entry
        if (passage == null) return Dungeon.customDungeon.getStart();
        return passage;
    }

    public Class<? extends Level> getType() {
        return type;
    }

    public Level getLevel() {
        return level;
    }

    public CustomDungeon getCustomDungeon() {
        return customDungeon;
    }

    public Level.Feeling getFeeling() {
        return feeling;
    }

    public boolean hasBoss() {
        return getBoss() != REGION_NONE;
    }

    public int generateGhostQuestNotRandom() {
        if (numInRegion <= 2) return GhostQuest.RAT;
        if (numInRegion == 3) return GhostQuest.GNOLL;
        return GhostQuest.CRAB;
    }

    public int generateWandmakerQuest() {
        return Random.Int(WandmakerQuest.NUM_QUESTS);
    }

    public int generateBlacksmithQuest() {
        return Random.Int(2);
    }

    public int generateImpQuestNotRandom() {
        //always assigns monks on floor 17, golems on floor 19, and 50/50 between either on 18
        if (numInRegion <= 2) return ImpQuest.MONK_QUEST;
        if (numInRegion >= 4) return ImpQuest.GOLEM_QUEST;
        return Random.Int(2);
    }

    public int getPriceMultiplier() {
        return shopPriceMultiplier;
    }

    public void setChasm(String chasm) {
        this.chasm = chasm;
    }

    public void setPassage(String passage) {
        this.passage = passage;
    }

    public void setEntranceTransitionRegular(LevelTransition entranceTransitionRegular) {
        this.entranceTransitionRegular = entranceTransitionRegular;
    }

    public void setExitTransitionRegular(LevelTransition exitTransitionRegular) {
        this.exitTransitionRegular = exitTransitionRegular;
    }

    public void setLevel(Level level) {
        this.level = level;
        type = level.getClass();
        level.levelScheme = this;
    }

    public void setFeeling(Level.Feeling feeling) {
        this.feeling = feeling;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setShopPriceMultiplier(int shopPriceMultiplier) {
        this.shopPriceMultiplier = shopPriceMultiplier;
    }

    public Level initLevel() {
        if (type != CustomLevel.class) {
            Random.pushGenerator(seed);
            Dungeon.levelName = name;
            level = Reflection.newInstance(type);
            Dungeon.level = level;
            level.name = name;
            level.levelScheme = this;
            level.feeling = feeling;
            initRandomStats(Random.Long());
            level.create();
            spawnItemsAndMobs(Random.Long());
            Random.popGenerator();
        } else {
            if (level == null) {
                try {
                    level = CustomDungeonSaves.loadLevel(name);
                } catch (IOException e) {
                    SandboxPixelDungeon.reportException(e);
                } catch (CustomDungeonSaves.RenameRequiredException e) {
                    throw new RuntimeException(e);//Caught by InterlevelScene
                }
            } else
                level = ((CustomLevel) level).createCopiedFloor();//make sure the levels are different objects? FIXME maybe not needed?? WICHTIG
            level.levelScheme = this;
            level.name = name;
            Dungeon.level = level;
            Dungeon.levelName = name;
            initRandomStats(seed);

            if (Dungeon.isChallenged(Challenges.DARKNESS)) {
                if (reduceViewDistanceIfDarkness) level.viewDistance /= 4;
                if (spawnTorchIfDarkness) itemsToSpawn.add(new Torch());
            }

            spawnItemsAndMobs(seed + 229203);

            if (Dungeon.isChallenged(Challenges.CHAMPION_ENEMIES) && rollForChampionIfChampionChallenge) {
                Random.pushGenerator(seed + 9488532);
                List<Mob> sortedMobs = new ArrayList<>(level.mobs);
                Collections.sort(sortedMobs, (m1, m2) -> m1.pos - m2.pos);
                for (Mob m : sortedMobs) {
                    if (m.buffs(ChampionEnemy.class).isEmpty()) {
                        ChampionEnemy.rollForChampion(m);
                    }
                }
                Random.popGenerator();
            }

            if (feeling == null) {
                Random.pushGenerator(seed + 56709);
                switch (Random.Int(14)) {
                    case 0:
                        level.feeling = Level.Feeling.CHASM;
                        break;
                    case 1:
                        level.feeling = Level.Feeling.WATER;
                        break;
                    case 2:
                        level.feeling = Level.Feeling.GRASS;
                        break;
                    case 3:
                        level.feeling = Level.Feeling.DARK;
                        break;
                    case 4:
                        level.feeling = Level.Feeling.LARGE;
                        break;
                    case 5:
                        level.feeling = Level.Feeling.TRAPS;
                        break;
                    case 6:
                        level.feeling = Level.Feeling.SECRETS;
                        break;
                    default:
                        level.feeling = Level.Feeling.NONE;
                }
                if (level.feeling == Level.Feeling.DARK)
                    level.viewDistance = Math.round(level.viewDistance / 2f);
                Random.popGenerator();
            } else level.feeling = feeling;
        }
        if (Dungeon.isChallenged(Challenges.NO_SCROLLS) && affectedByNoScrolls) {
            customDungeon.removeEverySecondSoU(level);
        }
        level.initForPlay();
        return level;
    }

    private void initRandomStats(long seed) {
        Random.pushGenerator(seed);

        for (Mob m : mobsToSpawn) {
            if (m instanceof QuestNPC) ((QuestNPC<?>) m).initQuest(this);
        }
        if (type == CustomLevel.class) {
            for (Mob m : level.mobs) {
                if (m instanceof QuestNPC) ((QuestNPC<?>) m).initQuest(this);
            }
        }
        Random.popGenerator();
    }

    private void spawnItemsAndMobs(long seed) {
        Random.pushGenerator(seed);

        if (!(level instanceof RegularLevel)) {
            for (Mob m : mobsToSpawn) {
                if (m.pos <= 0) {
                    int tries = level.length();
                    do {
                        m.pos = level.randomRespawnCell(m);
                        tries--;
                    } while (m.pos == -1 && tries > 0);
                    if (m.pos != -1) {
                        if (!(m instanceof NPC) && m.buffs(ChampionEnemy.class).isEmpty()) {
                            ChampionEnemy.rollForChampion(m);
                        }
                        level.mobs.add(m);
                        if (level.map[m.pos] == Terrain.HIGH_GRASS)
                            level.map[m.pos] = Terrain.GRASS;
                    }
                }
            }
        }
        if (type == CustomLevel.class) itemsToSpawn.addAll(prizeItemsToSpawn);

        for (Item item : itemsToSpawn) {
            item.reset();//important for scroll runes being inited
            int cell;
            if (level instanceof CustomLevel) cell = ((CustomLevel) level).randomDropCell();
            else if (level instanceof RegularLevel) cell = ((RegularLevel) level).randomDropCell();
            else cell = CustomLevel.randomDropCell(level);
            if (level.map[cell] == Terrain.HIGH_GRASS || level.map[cell] == Terrain.FURROWED_GRASS) {
                level.map[cell] = Terrain.GRASS;
                level.losBlocking[cell] = false;
            }
            Heap h = level.drop(item, cell);
            if (h.type == null) h.type = Heap.Type.HEAP;
        }

        Random.popGenerator();
    }


    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        seedSet = true;
    }

    public void initSeed() {
        if (!seedSet) {
            int lookAhead = depth;
//            if (name.equals(String.valueOf(depth)))
            lookAhead += name.hashCode() % 100;

            for (int i = 0; i < lookAhead; i++) {
                Random.Long(); //we don't care about these values, just need to go through them
            }
            seed = Random.Long();
        }
    }


    private static final String NAME = "name";
    private static final String ENTRANCE_CELLS = "entrance_cells";
    private static final String EXIT_CELLS = "exit_cells";
    private static final String CHASM = "chasm";
    private static final String DEST_ENTRANCE_REGULAR = "dest_entrance_regular";
    private static final String DEST_EXIT_REGULAR = "dest_exit_regular";
    private static final String PASSAGE = "passage";
    private static final String DEPTH = "depth";
    private static final String TYPE = "type";
    private static final String NUM_IN_REGION = "num_in_region";
    private static final String FEELING = "feeling";
    private static final String SEED = "seed";
    private static final String SEED_SET = "seed_set";
    private static final String SHOP_PRICE_MULTIPLIER = "shop_price_multiplier";

    private static final String MOBS_TO_SPAWN = "mobs_to_spawn";
    private static final String ROOMS_TO_SPAWN = "rooms_to_spawn";
    private static final String ITEMS_TO_SPAWN = "items_to_spawn";
    private static final String PRIZE_ITEMS_TO_SPAWN = "prize_items_to_spawn";
    private static final String SPAWN_STANDART_ROOMS = "spawn_standart_rooms";
    private static final String SPAWN_SECRET_ROOMS = "spawn_secret_rooms";
    private static final String SPAWN_SPECIAL_ROOMS = "spawn_special_rooms";
    private static final String SPAWN_MOBS = "spawn_mobs";
    private static final String SPAWN_ITEMS = "spawn_items";
    private static final String BUILDER = "builder";
    private static final String SPAWN_TORCH_IF_DARKNESS = "spawn_torch_if_darkness";
    private static final String REDUCE_VIEW_DISTANCE_IF_DARKNESS = "reduce_view_distance_if_darkness";
    private static final String AFFECTED_BY_NO_SCROLLS = "affected_by_no_scrolls";
    private static final String ROLL_FOR_CHAMPION_IF_CHAMPION_CHALLENGE = "roll_for_champion_if_champion_challenge";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(NAME, name);
        bundle.put(CHASM, chasm);
        if (entranceTransitionRegular != null)
            bundle.put(DEST_ENTRANCE_REGULAR, entranceTransitionRegular);
        if (exitTransitionRegular != null) bundle.put(DEST_EXIT_REGULAR, exitTransitionRegular);
        bundle.put(PASSAGE, passage);
        bundle.put(DEPTH, depth);
        bundle.put(TYPE, type);
        bundle.put(NUM_IN_REGION, numInRegion);
        bundle.put(SEED, seed);
        bundle.put(SEED_SET, seedSet);
        bundle.put(FEELING, feeling);
        bundle.put(SHOP_PRICE_MULTIPLIER, shopPriceMultiplier);
        bundle.put(SPAWN_TORCH_IF_DARKNESS, spawnTorchIfDarkness);
        bundle.put(REDUCE_VIEW_DISTANCE_IF_DARKNESS, reduceViewDistanceIfDarkness);
        bundle.put(AFFECTED_BY_NO_SCROLLS, affectedByNoScrolls);
        bundle.put(ROLL_FOR_CHAMPION_IF_CHAMPION_CHALLENGE, rollForChampionIfChampionChallenge);

        int[] entrances = new int[entranceCells.size()];
        int i = 0;
        for (int cell : entranceCells) {
            entrances[i] = cell;
            i++;
        }
        bundle.put(ENTRANCE_CELLS, entrances);
        int[] exits = new int[exitCells.size()];
        i = 0;
        for (int cell : exitCells) {
            exits[i] = cell;
            i++;
        }
        bundle.put(EXIT_CELLS, exits);

        bundle.put(MOBS_TO_SPAWN, mobsToSpawn);
        bundle.put(ITEMS_TO_SPAWN, itemsToSpawn);
        bundle.put(PRIZE_ITEMS_TO_SPAWN, prizeItemsToSpawn);
        bundle.put(ROOMS_TO_SPAWN, roomsToSpawn);
        bundle.put(SPAWN_STANDART_ROOMS, spawnStandartRooms);
        bundle.put(SPAWN_SPECIAL_ROOMS, spawnSpecialRooms);
        bundle.put(SPAWN_SECRET_ROOMS, spawnSecretRooms);
        bundle.put(SPAWN_MOBS, spawnMobs);
        bundle.put(SPAWN_ITEMS, spawnItems);
        if (builder != null) bundle.put(BUILDER, builder);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        name = bundle.getString(NAME);
        chasm = bundle.getString(CHASM);
        if (bundle.contains(DEST_ENTRANCE_REGULAR))
            entranceTransitionRegular = (LevelTransition) bundle.get(DEST_ENTRANCE_REGULAR);
        if (bundle.contains(DEST_EXIT_REGULAR))
            exitTransitionRegular = (LevelTransition) bundle.get(DEST_EXIT_REGULAR);
        passage = bundle.getString(PASSAGE);
        depth = bundle.getInt(DEPTH);
        if (bundle.contains(FEELING)) feeling = bundle.getEnum(FEELING, Level.Feeling.class);
        shopPriceMultiplier = bundle.getInt(SHOP_PRICE_MULTIPLIER);
        spawnTorchIfDarkness = bundle.getBoolean(SPAWN_TORCH_IF_DARKNESS);
        reduceViewDistanceIfDarkness = bundle.getBoolean(REDUCE_VIEW_DISTANCE_IF_DARKNESS);
        affectedByNoScrolls = bundle.getBoolean(AFFECTED_BY_NO_SCROLLS);
        rollForChampionIfChampionChallenge = bundle.getBoolean(ROLL_FOR_CHAMPION_IF_CHAMPION_CHALLENGE);

        int[] entrances = bundle.getIntArray(ENTRANCE_CELLS);
        entranceCells = new ArrayList<>(entrances.length + 1);
        for (int entrance : entrances) entranceCells.add(entrance);
        Collections.sort(entranceCells);//should actually already be sorted

        int[] exits = bundle.getIntArray(EXIT_CELLS);
        exitCells = new ArrayList<>(exits.length + 1);
        for (int exit : exits) exitCells.add(exit);
        Collections.sort(exitCells);

        type = bundle.getClass(TYPE);
        numInRegion = bundle.getInt(NUM_IN_REGION);
        seed = bundle.getLong(SEED);
        seedSet = bundle.getBoolean(SEED_SET);

        mobsToSpawn = new ArrayList<>();
        if (bundle.contains(MOBS_TO_SPAWN))
            for (Bundlable l : bundle.getCollection(MOBS_TO_SPAWN)) mobsToSpawn.add((Mob) l);

        itemsToSpawn = new ArrayList<>();
        if (bundle.contains(ITEMS_TO_SPAWN))
            for (Bundlable l : bundle.getCollection(ITEMS_TO_SPAWN)) itemsToSpawn.add((Item) l);

        prizeItemsToSpawn = new ArrayList<>();
        if (bundle.contains(PRIZE_ITEMS_TO_SPAWN))
            for (Bundlable l : bundle.getCollection(PRIZE_ITEMS_TO_SPAWN))
                prizeItemsToSpawn.add((Item) l);

        roomsToSpawn = new ArrayList<>();
        if (bundle.contains(ROOMS_TO_SPAWN))
            for (Bundlable l : bundle.getCollection(ROOMS_TO_SPAWN)) roomsToSpawn.add((Room) l);

        spawnStandartRooms = bundle.getBoolean(SPAWN_STANDART_ROOMS);
        spawnSecretRooms = bundle.getBoolean(SPAWN_SECRET_ROOMS);
        spawnSpecialRooms = bundle.getBoolean(SPAWN_SPECIAL_ROOMS);
        spawnMobs = bundle.getBoolean(SPAWN_MOBS);
        spawnItems = bundle.getBoolean(SPAWN_ITEMS);
        builder = bundle.getClass(BUILDER);
    }

    public Point getSizeIfUnloaded() {
        boolean unloadLevel = level == null;
        if (unloadLevel) loadLevel();
        if (level == null) return new Point(-1, -1);//For non CustomLevels
        Point ret = new Point(level.width(), level.height());
        if (unloadLevel) unloadLevel();
        return ret;
    }

    public Level loadLevel() {
        return loadLevel(true);
    }

    public Level loadLevel(boolean removeInvalidTransitions) {
        if (type == CustomLevel.class) {
            try {
                level = CustomDungeonSaves.loadLevel(name, removeInvalidTransitions);
                level.levelScheme = this;
            } catch (IOException e) {
                SandboxPixelDungeon.reportException(e);
            }
            catch (CustomDungeonSaves.RenameRequiredException ex){
                ex.showExceptionWindow();
            }
        }
        return level;
    }

    public void saveLevel() throws IOException {
        if (type == CustomLevel.class) CustomDungeonSaves.saveLevel(level);
    }

    public void unloadLevel() {
        level = null;
    }


    public static final int REGION_NONE = 0, REGION_SEWERS = 1, REGION_PRISON = 2, REGION_CAVES = 3, REGION_CITY = 4, REGION_HALLS = 5;

    public static int getRegion(Level level) {
        if (level instanceof CustomLevel) return ((CustomLevel) level).region;
        if (level instanceof SewerLevel) return REGION_SEWERS;
        if (level instanceof PrisonLevel || level instanceof PrisonBossLevel) return REGION_PRISON;
        if (level instanceof CavesLevel || level instanceof CavesBossLevel) return REGION_CAVES;
        if (level instanceof CityLevel || level instanceof CityBossLevel) return REGION_CITY;
        if (level instanceof HallsLevel || level instanceof HallsBossLevel || level instanceof LastLevel || level instanceof DeadEndLevel)
            return REGION_HALLS;
        return REGION_NONE;
    }

    public static int getRegion(Class<? extends Level> level) {
        if (level == SewerLevel.class || level == SewerBossLevel.class) return REGION_SEWERS;
        if (level == PrisonLevel.class || level == PrisonBossLevel.class) return REGION_PRISON;
        if (level == CavesLevel.class || level == CavesBossLevel.class) return REGION_CAVES;
        if (level == CityLevel.class || level == CityBossLevel.class) return REGION_CITY;
        if (level == HallsLevel.class || level == HallsBossLevel.class || level == LastLevel.class || level == DeadEndLevel.class)
            return REGION_HALLS;
        return REGION_NONE;
    }

    public static String getRegionTexture(int region) {
        switch (region) {
            case REGION_SEWERS:
                return Assets.Environment.TILES_SEWERS;
            case REGION_PRISON:
                return Assets.Environment.TILES_PRISON;
            case REGION_CAVES:
                return Assets.Environment.TILES_CAVES;
            case REGION_CITY:
                return Assets.Environment.TILES_CITY;
            case REGION_HALLS:
                return Assets.Environment.TILES_HALLS;
        }
        return null;
    }

    public static int getBoss(Class<? extends Level> level) {
        if (level == SewerBossLevel.class) return REGION_SEWERS;
        if (level == PrisonBossLevel.class) return REGION_PRISON;
        if (level == CavesBossLevel.class) return REGION_CAVES;
        if (level == CityBossLevel.class) return REGION_CITY;
        if (level == HallsBossLevel.class) return REGION_HALLS;
        return REGION_NONE;
    }

    public final int getRegion() {
        if (level == null) return getRegion(type);
        return getRegion(level);
    }

    public final int getBoss() {
//        if (level instanceof Floor) return ((Floor) level).region;
        return getBoss(level == null ? type : level.getClass());
    }

    public int getNumInRegion() {//if it is the first, second, 3rd etc floor of a region (only good for setting default playing)
        return numInRegion;
    }

    @Override
    public int compareTo(LevelScheme o) {
        if (getDepth() != o.getDepth())
            return (getDepth() - o.getDepth()) * 1000;//TODO might wanna use more complex sorting, like used for ItemTab
        return getName().compareTo(o.getName());
    }

    //Depth nur für Scaling, Region und numInRegionFür LevelGen
}