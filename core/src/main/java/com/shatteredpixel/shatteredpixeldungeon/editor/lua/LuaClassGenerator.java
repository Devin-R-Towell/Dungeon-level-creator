/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Sandbox Pixel Dungeon
 * Copyright (C) 2023-2024 AlphaDraxonis
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.editor.lua;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.EditorInvCategory;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.Mobs;
import com.shatteredpixel.shatteredpixeldungeon.editor.levels.CustomLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class LuaClassGenerator {

    public static void enterClass(){
        int i = 0;
    }

    private static final String funAttackSkill =
            "function attackSkill(this) " +
                    "    return 999999" +
                    " end  ";
    private static final String funAttackProc =
            "function attackProc(this, enemy, damage) " +
                    "    this:die()" +
                    "    return damage" +
                    " end  ";
    private static final String funAttackProc2 =
            "function attackProc(this, enemy, damage) " +
                    "    test = test + 1" +
                    "    return test" +
                    " end  ";
    private static final String funDie =
            "function die(this, vars, super, cause) " +
                    "local item = luajava.newInstance(\"com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost\")" +
                    "level:drop(item, this.pos + level:width()).sprite:drop()" +
                    "super:call({cause})" +
                    " end  ";

    private static final String vars =
            "vars = { " +
//                    "local item = luajava.newInstance(\"com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost\")" +
                    "item = nil;" +
                    "test = 11;" +
                    "static = {" +
                    "aNumber = 17" +
                    "};" +
                    "globals = {" +
                    "globalValue = 99" +
                    "}" +
                    " }  ";

    private static final String ROOT_DIR = System.getProperty("user.home")
            + "/ZZDaten/Freizeit/Programmieren/ShatteredPD/SPD-Sandbox/Projekt/core/src/main/java/";
//            Mob.class.getPackage().getName().replace('.', '/') + "/luamobs/";

    private LuaClassGenerator() {
    }

    public static void generateLevelSourceFiles() {
        generateLevelFile(CavesBossLevel.class);
        generateLevelFile(CavesLevel.class);
        generateLevelFile(CavesBossLevel.class);
        generateLevelFile(CityBossLevel.class);
        generateLevelFile(CityLevel.class);
        generateLevelFile(DeadEndLevel.class);
        generateLevelFile(HallsBossLevel.class);
        generateLevelFile(HallsLevel.class);
        generateLevelFile(LastLevel.class);
        generateLevelFile(LastShopLevel.class);
        generateLevelFile(MiningLevel.class);
        generateLevelFile(PrisonBossLevel.class);
        generateLevelFile(PrisonLevel.class);
        generateLevelFile(SewerBossLevel.class);
        generateLevelFile(SewerLevel.class);
        generateLevelFile(CustomLevel.class);
    }

    public static void generateMobFiles() {

        Class<?>[][] mobs = EditorInvCategory.getAll(Mobs.values());

        for (Class<?>[] classes : mobs) {
            for (Class<?> c : classes) {
                generateMobFile(c);
            }
        }
    }

    private static void generateLevelFile(Class<?> inputClass) {
        String source = generateSourceCodeLevel(inputClass);

        String path = ROOT_DIR
                + (Level.class.getPackage().getName() + ".lualevels.").replaceAll("\\.", "/")
                + inputClass.getSimpleName() + "_lua.java";

        File f = new File(path);

        if (f.exists()) f.delete();

		try {
			f.createNewFile();
		} catch (IOException e) {
            e.printStackTrace();
		}

		try (FileWriter writer = new FileWriter(f)){
            writer.write(source);
        } catch (IOException e) {
            e.printStackTrace();
		}
    }

    private static void generateMobFile(Class<?> inputClass) {
        String source = generateSourceCodeMob(inputClass);

        String path = ROOT_DIR
                + (Mob.class.getPackage().getName() + ".luamobs.").replaceAll("\\.", "/")
                + inputClass.getSimpleName() + "_lua.java";

        File f = new File(path);

        if (f.exists()) f.delete();

        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileWriter writer = new FileWriter(f)){
            writer.write(source);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //private LuaValue luaVars;//LuaTable mit variablen, wird gespeichert,  bei restoreFromBundle() werden nur die Werte übernommen, die tatsächlich noch vorhanden sind
    //
    //    {
    //
    ////        //TODO tzz find better way of copying, extract methods and make static
    ////        LuaTable originalVars = LuaClassGenerator.luaScript.get("vars").checktable();
    ////        luaVars = LuaClassGenerator.deepCopyLuaValue(originalVars);
    //
    //        //in restoreFromBundle: check what has been saved, and only override those vars that are still present
    //        //in storeInBundle: find a way to properly store all of that automatically
    //        //test if they are separate

    public static String generateSourceCodeMob(Class<?> inputClass) {
        String pckge = "package " + Mob.class.getPackage().getName() + ".luamobs;\n\n";
        String imprt = "import " + Messages.MAIN_PACKAGE_NAME + "actors.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.buffs.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.mobs.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.mobs.npcs.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.hero.Hero;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.levels.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.lua.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.ui.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.util.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "items.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "items.armor.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "items.weapon.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "items.wands.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.rooms.special.SentryRoom;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "sprites.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "windows.WndError;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "ui.Window;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "scenes.DungeonScene;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "GameObject;\n" +
                "import com.watabou.noosa.Game;\n" +
                "import com.watabou.utils.*;\n" +
                "import org.luaj.vm2.*;\n" +
                "import org.luaj.vm2.lib.jse.CoerceJavaToLua;\n" +
                "import java.util.*;\n\n";
        String extents = inputClass.getSimpleName();
        if (inputClass.getEnclosingClass() != null) extents = inputClass.getEnclosingClass().getSimpleName() + "." + extents;
        String classHead = "public class " + inputClass.getSimpleName() + "_lua extends " + extents +" implements LuaMob {\n\n";
        String declaringVars = "    private int identifier;\n"
                + "    private boolean inheritsStats = true;\n"
                + "    private LuaTable vars;\n";
        String implementLuaClassStuff =
                "\n" +
                        "    @Override\n" +
                        "    public void setIdentifier(int identifier) {\n" +
                        "        this.identifier = identifier;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public int getIdentifier() {\n" +
                        "        return this.identifier;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void setInheritsStats(boolean inheritsStats) {\n" +
                        "        this.inheritsStats = inheritsStats;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public boolean getInheritsStats() {\n" +
                        "        return inheritsStats;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public LuaClass newInstance() {\n" +
                        "        return (LuaClass) getCopy();\n" +
                        "    }\n\n";
        String bundlingMethods =
                "@Override\n" +
                        "    public void storeInBundle(Bundle bundle) {\n" +
                        "        super.storeInBundle(bundle);\n" +
                        "        bundle.put(LuaClass.IDENTIFIER, identifier);\n" +
                        "        bundle.put(LuaMob.INHERITS_STATS, inheritsStats);\n" +
                        "        if (vars != null && !CustomDungeon.isEditing()) {\n" +
                        "            LuaManager.storeVarInBundle(bundle, vars, VARS);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void restoreFromBundle(Bundle bundle) {\n" +
                        "        super.restoreFromBundle(bundle);\n" +
                        "        identifier = bundle.getInt(LuaClass.IDENTIFIER);\n" +
                        "        inheritsStats = bundle.getBoolean(LuaMob.INHERITS_STATS);\n" +
                        "\n" +
                        "        LuaValue script;\n" +
                        "        if (!CustomDungeon.isEditing() && (script = CustomObject.getScript(identifier)) != null && script.get(\"vars\").istable()) {\n" +
                        "            vars = LuaManager.deepCopyLuaValue(script.get(\"vars\")).checktable();\n" +
                        "\n" +
                        "            LuaValue loaded = LuaManager.restoreVarFromBundle(bundle, VARS);\n" +
                        "            if (loaded != null && loaded.istable()) vars = loaded.checktable();\n" +
                        "            if (script.get(\"static\").istable()) vars.set(\"static\", script.get(\"static\"));\n" +
                        "        }\n" +
                        "    }\n\n";
        //TODO tzz don't forget Methods from Bundlable!
        //And tell: if you want to store sth like the currently targeted enemy, store the id and ...

        Map<String, Method> methods = new HashMap<>();
        findAllMethodsToOverride(inputClass, Actor.class, methods);
        methods.remove("storeInBundle");
        methods.remove("restoreFromBundle");

        methods.remove("getCopy");
        methods.remove("onRenameLevelScheme");
        methods.remove("onDeleteLevelScheme");
        methods.remove("setDurationForBuff");
        methods.remove("moveBuffSilentlyToOtherChar_ACCESS_ONLY_FOR_HeroMob");
        methods.remove("getPropertiesVar_ACCESS_ONLY_FOR_EDITING_UI");
        methods.remove("spend_DO_NOT_CALL_UNLESS_ABSOLUTELY_NECESSARY");
        methods.remove("setFirstAddedToTrue_ACCESS_ONLY_FOR_CUSTOMLEVELS_THAT_ARE_ENTERED_FOR_THE_FIRST_TIME");

        return pckge
                + imprt
                + classHead
                + declaringVars
                + implementLuaClassStuff
                + bundlingMethods
                + overrideMethods(methods.values(), "CustomObject.getScript(identifier)")
                + "}";
    }

    public static String generateSourceCodeLevel(Class<?> inputClass) {
        String accessScript = "levelScheme.luaScript.getScript()";
        String pckge = "package " + Level.class.getPackage().getName() + ".lualevels;\n\n";
        String imprt = "import " + Messages.MAIN_PACKAGE_NAME + "actors.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.buffs.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.mobs.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "actors.hero.Hero;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.levels.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.lua.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.ui.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "editor.util.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "items.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "sprites.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "windows.WndError;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "scenes.DungeonScene;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.builders.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.painters.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.features.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.rooms.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "levels.traps.*;\n" +
                "import " + Messages.MAIN_PACKAGE_NAME + "plants.Plant;\n" +
                "import com.watabou.noosa.Game;\n" +
                "import com.watabou.utils.Bundle;\n" +
                "import org.luaj.vm2.*;\n" +
                "import org.luaj.vm2.lib.jse.CoerceJavaToLua;\n" +
                "import java.util.*;\n\n";
        String classHead = "public class " + inputClass.getSimpleName() + "_lua extends " + inputClass.getSimpleName() +" implements LuaLevel {\n\n";
        String declaringVars = "    private LuaTable vars;\n";
        String bundlingMethods =
                        "    @Override\n" +
                        "    public void setVars(LuaValue vars) {\n" +
                        "        this.vars = vars;\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void storeInBundle(Bundle bundle) {\n" +
                        "        super.storeInBundle(bundle);\n" +
                        "        if (vars != null) {\n" +
                        "            LuaManager.storeVarInBundle(bundle, vars, LuaClass.VARS);\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    @Override\n" +
                        "    public void restoreFromBundle(Bundle bundle) {\n" +
                        "        super.restoreFromBundle(bundle);\n" +
                        "\n" +
                        "        LuaValue loaded = LuaManager.restoreVarFromBundle(bundle, LuaClass.VARS);\n" +
                        "        if (loaded != null && loaded.istable()) vars = loaded.checktable();\n" +
                        "    }\n\n";

        Map<String, Method> methods = new HashMap<>();
        findAllMethodsToOverride(inputClass, Level.class, methods);
        methods.remove("storeInBundle");
        methods.remove("restoreFromBundle");

        methods.remove("setSize");
        methods.remove("width");
        methods.remove("height");
        methods.remove("tilesTex");
        methods.remove("waterTex");
        methods.remove("getTransition");
        methods.remove("addVisuals");
        methods.remove("addWallVisuals");
        methods.remove("findMob");
        methods.remove("addRespawner");
        methods.remove("addZoneRespawner");
        methods.remove("buildFlagMaps");
        methods.remove("isPassable");
        methods.remove("isPassableHero");
        methods.remove("isPassableMob");
        methods.remove("isPassableAlly");
        methods.remove("getPassableVar");
        methods.remove("getPassableHeroVar");
        methods.remove("getPassableMobVar");
        methods.remove("getPassableAllyVar");
        methods.remove("getPassableAndAnyVarForBoth");
        methods.remove("getPassableAndAvoidVar");
        methods.remove("removeSimpleCustomTile");
        methods.remove("cleanWalls");
        methods.remove("cleanWallCell");
        methods.remove("distance");
        methods.remove("adjacent");
        methods.remove("trueDistance");
        methods.remove("insideMap");
        methods.remove("cellToPoint");
        methods.remove("pointToCell");
        methods.remove("appendNoTransWarning");

        //CustomLevel
        methods.remove("updateTransitionCells");

//        methods.remove("getCopy");
//        methods.remove("onRenameLevelScheme");
//        methods.remove("onDeleteLevelScheme");
//        methods.remove("setDurationForBuff");
//        methods.remove("moveBuffSilentlyToOtherChar_ACCESS_ONLY_FOR_HeroMob");
//        methods.remove("getPropertiesVar_ACCESS_ONLY_FOR_EDITING_UI");
//        methods.remove("spend_DO_NOT_CALL_UNLESS_ABSOLUTELY_NECESSARY");
//        methods.remove("setFirstAddedToTrue_ACCESS_ONLY_FOR_CUSTOMLEVELS_THAT_ARE_ENTERED_FOR_THE_FIRST_TIME");

        return pckge
                + imprt
                + classHead
                + declaringVars
                + bundlingMethods
                + overrideMethods(methods.values(), accessScript)
                + "}";
    }

    private static String overrideMethods(Collection<Method> methods, String accessScript) {
        StringBuilder overrideMethods = new StringBuilder();

        for (Method m : methods) {

            Class<?> returnType = m.getReturnType();
            String returnTypeName = className(returnType);

            overrideMethods.append('\n');
            overrideMethods.append("    @Override\n    ");
            overrideMethods.append(Modifier.toString(m.getModifiers())).append(" ");

            overrideMethods.append(returnTypeName).append(" ");
            overrideMethods.append(m.getName()).append("(");

            Class<?>[] paramTypes = m.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                overrideMethods.append(className(paramTypes[i]));
                overrideMethods.append(" arg").append(i);
                if (i < paramTypes.length - 1)
                    overrideMethods.append(", ");
            }

            String returnString = returnType == void.class ? "" : returnTypeName + " ret = ";
            if (!returnType.isPrimitive() && returnType != String.class) returnString += "(" + returnTypeName + ") ";
            String returnTypeString;
            if (returnType == String.class) returnTypeString = ".tojstring()";
            else if (returnType == void.class) returnTypeString = "";
            else if (returnType.isPrimitive()) returnTypeString = ".to" + returnType + "()";
            else returnTypeString = ".touserdata()";

            boolean useInvoke = paramTypes.length >= 1;

            overrideMethods.append(") {\n");
            overrideMethods.append("        LuaValue luaScript = " + accessScript + ";\n");
            overrideMethods.append("        if (luaScript != null && !luaScript.get(\"").append(m.getName()).append("\").isnil()) {\n");

            overrideMethods.append("            try {\n");
            overrideMethods.append("                MethodOverride");
            if (paramTypes.length <= 10) overrideMethods.append('.');
            if (returnType == void.class) overrideMethods.append("Void");
            if (paramTypes.length <= 10) overrideMethods.append('A').append(paramTypes.length);
            if (returnType != void.class) {
                overrideMethods.append('<');
                if (returnType.isPrimitive()) {
                    if (returnType == int.class) overrideMethods.append("Integer");
                    else if (returnType == char.class) overrideMethods.append("Character");
                    else overrideMethods.append(Messages.capitalize(returnTypeName));
                }
                else overrideMethods.append(returnTypeName);
                overrideMethods.append('>');
            }

            overrideMethods.append(" superMethod = (");
            for (int i = 0; i < paramTypes.length; i++) {
                overrideMethods.append('a').append(i);
                if (i + 1 < paramTypes.length) overrideMethods.append(", ");
            }
            overrideMethods.append(") -> super.");
            overrideMethods.append(m.getName()).append('(');
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] != Object.class) {
                    overrideMethods.append('(');
                    overrideMethods.append(className(paramTypes[i]));
                    overrideMethods.append(')');
                    overrideMethods.append(' ');
                }
                overrideMethods.append('a').append(i);
                if (i+1 < paramTypes.length) overrideMethods.append(", ");
            }
            overrideMethods.append(");\n");

            overrideMethods.append("               ").append(returnString).append("luaScript.get(\"")
                    .append(m.getName()).append("\").").append(useInvoke ? "invoke" : "call").append('(');

            if (useInvoke) overrideMethods.append("new LuaValue[]{");
            overrideMethods.append("CoerceJavaToLua.coerce(this), ");
            overrideMethods.append("vars, ");
            overrideMethods.append("CoerceJavaToLua.coerce(superMethod)");
            if (paramTypes.length > 0) overrideMethods.append(", ");

            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i].isPrimitive()) {
                    overrideMethods.append("LuaValue.valueOf(");
                } else {
                    overrideMethods.append("CoerceJavaToLua.coerce(");
                }
                overrideMethods.append("arg").append(i);
                overrideMethods.append(")");
                if (i < paramTypes.length - 1)
                    overrideMethods.append(", ");
            }

            if (useInvoke) overrideMethods.append('}');
            overrideMethods.append(')');
            if (useInvoke) overrideMethods.append(".arg1()");
            overrideMethods.append(returnTypeString).append(";\n");
            if (!returnString.isEmpty())
                overrideMethods.append("                return ret;\n");
            overrideMethods.append("            } catch (LuaError error) { Game.runOnRenderThread(()->\tDungeonScene.show(new WndError(error))); }\n");
            overrideMethods.append("        }\n");

            overrideMethods.append("        ");
            if (returnType != void.class) {
                overrideMethods.append("return ");
            }
            overrideMethods.append("super.").append(m.getName()).append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                overrideMethods.append("arg").append(i);
                if (i < paramTypes.length - 1)
                    overrideMethods.append(", ");
            }
            overrideMethods.append(");\n");

            overrideMethods.append("    }\n");

        }

        return overrideMethods.toString();
    }

    private static String className(Class<?> clazz) {
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass != null && Modifier.isStatic(clazz.getModifiers())) {
            return className(enclosingClass) + "." + clazz.getSimpleName();
        }
        return clazz.getSimpleName();
    }

    private static void findAllMethodsToOverride(Class<?> currentClass, Class<?> highestClass, Map<String, Method> currentMethods) {
        for (Method m : currentClass.getDeclaredMethods()) {
            int mods = m.getModifiers();
            if (Modifier.isPrivate(mods) || Modifier.isFinal(mods) || Modifier.isStatic(mods)) {
                //don't override these
                continue;
            }
            if (!currentMethods.containsKey(m.getName()))
                currentMethods.put(m.getName(), m);
        }
        if (currentClass != highestClass) {
            findAllMethodsToOverride(currentClass.getSuperclass(), highestClass, currentMethods);
        }
    }



//    public static LuaTable updateTable(LuaTable returnTable, LuaTable dataSupplier) {
//        if (returnTable != null && dataSupplier != null) {
//            for (LuaValue k : dataSupplier.keys()) {
//                String keyAsString = k.toString();
//                LuaValue v = returnTable.get(keyAsString);
//                if (!v.isnil()) {
//                    returnTable.set(keyAsString, dataSupplier.get(keyAsString));
//                } else {
//
//                }
//            }
//        }
//        return returnTable;
//    }




//    private static final String VARS = "vars";
//
//    @Override
//    public void storeInBundle(Bundle bundle) {
//        super.storeInBundle(bundle);
//        if (vars != null) {
//            LuaClassGenerator.storeVarInBundle(bundle, vars, VARS);
//        }
//    }
//
//    //TODO tzz test: what happens if a bundlable is NOT stored as null in bundle, but IS in default script???
//    @Override
//    public void restoreFromBundle(Bundle bundle) {
//        super.restoreFromBundle(bundle);
//        LuaValue loaded = LuaClassGenerator.restoreVarFromBundle(bundle, VARS);
//        if (loaded != null && loaded.istable()) {
//            vars = LuaClassGenerator.updateTable(loaded.checktable(), vars);
//            if (LuaClassGenerator.luaScript != null && LuaClassGenerator.luaScript.get("vars").istable()) {
//              vars.set("static", LuaClassGenerator.luaScript.get("vars").get("static"));
//              vars.set("globals", LuaClassGenerator.luaScript.get("vars").get("globals"));
//          }
//        }
//    }

}

//    private static Map<String, Class<? extends LuaClass>> luaClassesCache = new HashMap<>();
//
//    public static Class<? extends LuaClass> forName(String inputClass) {
//        return generateLuaClass(Reflection.forName(inputClass.replace("_lua", "")));
//    }
//
//    public static Class<? extends LuaClass> generateLuaClass(Class<?> inputClass) {
//
//        String fullyQualifiedName = inputClass.getName() + "_lua";
//        if (luaClassesCache.containsKey(fullyQualifiedName)) {
//            return luaClassesCache.get(fullyQualifiedName);
//        }
//
//        String classSource = generateClassSource(inputClass);
//        Class<? extends LuaClass> generatedClass = compileClass(fullyQualifiedName, classSource);
//
//        luaClassesCache.put(fullyQualifiedName, generatedClass);
//
//        return generatedClass;
//    }
//
//    private static String generateClassSource(Class<?> inputClass) {
//        String package2 = inputClass.getPackage().getName();
//        package2 = "com.watabou.utils";
//        String source = "package " + package2 + ";\n" +
//                "public class " + inputClass.getSimpleName() + "_lua implements com.shatteredpixel.shatteredpixeldungeon.editor.lua.LuaClass {\n" +
//                "    private String identifier;\n" +
//                "\n" +
//                "    public void setIdentifier(String identifier) {\n" +
//                "        this.identifier = identifier;\n" +
//                "    }\n" +
//                "\n" +
//                "    public String getIdentifier() {\n" +
//                "        return this.identifier;\n" +
//                "    }\n" +
//                "}";
//
//        return source;
//    }
//
//    private static Class<? extends LuaClass> compileClass(String className, String classSource) {
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        MemoryFileManager fileManager = new MemoryFileManager(compiler.getStandardFileManager(null, null, null));
//        JavaFileObject javaFileObject = new MemoryJavaSourceFile(className, classSource);
//
//        compiler.getTask(null, fileManager, null, null, null, Collections.singletonList(javaFileObject)).call();
//
//        try {
//            return (Class<? extends LuaClass>) fileManager.getClassLoader(null).loadClass(className);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException("Failed to load compiled class: " + className, e);
//        }
//    }
//}
//
//class MemoryJavaClassFile extends SimpleJavaFileObject {
//    private final ByteArrayOutputStream outputStream;
//
//    MemoryJavaClassFile(String name) {
//        super(URI.create("file:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
//        outputStream = new ByteArrayOutputStream();
//    }
//
//    @Override
//    public OutputStream openOutputStream() {
//        return outputStream;
//    }
//
//    byte[] getBytes() {
//        return outputStream.toByteArray();
//    }
//}
//
//// Utility class to represent Java source code in memory
//class MemoryJavaSourceFile extends SimpleJavaFileObject {
//    private final CharSequence source;
//
//    MemoryJavaSourceFile(String name, CharSequence source) {
//        super(URI.create("file:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
//        this.source = source;
//    }
//
//    @Override
//    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
//        return source;
//    }
//}
//
//// Utility class to compile and load classes in memory
//class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
//    private final MemoryClassLoader classLoader;
//
//    MemoryFileManager(StandardJavaFileManager fileManager) {
//        super(fileManager);
//        classLoader = new MemoryClassLoader();
//    }
//
//    @Override
//    public ClassLoader getClassLoader(Location location) {
//        return classLoader;
//    }
//
//    @Override
//    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
//        return new MemoryJavaClassFile(className);
//    }
//
//    // Utility class to load classes from compiled bytes
//    private static class MemoryClassLoader extends ClassLoader {
//        private final Map<String, MemoryJavaClassFile> classFileMap = new HashMap<>();
//        void addClassFile(String className, MemoryJavaClassFile classFile) {
//            classFileMap.put(className, classFile);
//        }
//        @Override
//        protected Class<?> findClass(String name) throws ClassNotFoundException {
//            MemoryJavaClassFile file = classFileMap.get(name);
//            if (file != null) {
//                byte[] bytes = file.getBytes();
//                return defineClass(name, bytes, 0, bytes.length);
//            }
//            throw new ClassNotFoundException(name);
//        }
//    }
//}