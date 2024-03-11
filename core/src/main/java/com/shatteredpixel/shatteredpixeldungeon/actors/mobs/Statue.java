/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.editor.editcomps.parts.mobs.ItemSelectables;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.other.RandomItem;
import com.shatteredpixel.shatteredpixeldungeon.editor.levels.CustomDungeon;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.ItemSelector;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Statue extends Mob implements MobBasedOnDepth, ItemSelectables.WeaponSelectable {

	{
		spriteClass = StatueSprite.class;

		EXP = 0;
		state = PASSIVE;

		properties.add(Property.INORGANIC);
	}

	protected Weapon weapon;

	public boolean levelGenStatue = true;
	
	public Statue() {
		super();
		setLevel(Dungeon.depth);
		createItems(false);
	}

	@Override
	public void initRandoms() {
		super.initRandoms();
		createItems(false);
		weapon(RandomItem.initRandomStatsForItemSubclasses(weapon()));
	}

	@Override
	public void setLevel(int depth) {
		HT = (int) (15 + depth * 5 * statsScale);
		defenseSkill = 4 + depth;

		if (!hpSet) {
			HP = HT;
			hpSet = true;
		}
	}

	@Override
	public ItemSelector.NullTypeSelector useNullWeapon() {
		return ItemSelector.NullTypeSelector.RANDOM;
	}

	@Override
	protected void onAdd() {
		setLevel(Dungeon.depth);
		super.onAdd();
	}

	@Override
	public void weapon(Weapon weapon) {
		this.weapon = weapon;
	}

	@Override
	public Weapon weapon() {
		return weapon;
	}

	public void createItems(boolean useDecks ){
		if (weapon == null) {
			if (useDecks) {
				weapon = (MeleeWeapon) Generator.random(Generator.Category.WEAPON);
			} else {
				weapon = (MeleeWeapon) Generator.randomUsingDefaults(Generator.Category.WEAPON);
			}
			levelGenStatue = useDecks;
			weapon.cursed = false;
			weapon.enchant(Enchantment.random());
		}
	}

	private static final String WEAPON	= "weapon";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( WEAPON, weapon );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		weapon = (Weapon)bundle.get( WEAPON );
	}
	
	@Override
	protected boolean act() {
		if (levelGenStatue && Dungeon.level.visited[pos]) {
			Notes.add( Notes.Landmark.STATUE );
		}
		return super.act();
	}
	
	@Override
	public int damageRoll() {
		return (int) (weapon.damageRoll(this) * statsScale);
	}
	
	@Override
	public int attackSkill( Char target ) {
		return (int) ((int) ((9 + Dungeon.depth) * weapon.accuracyFactor(this, target)) * statsScale);
	}
	
	@Override
	public float attackDelay() {
		return super.attackDelay()*weapon.delayFactor( this );
	}

	@Override
	protected boolean canAttack(Char enemy) {
		return super.canAttack(enemy) || weapon.canReach(this, enemy.pos);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, Dungeon.depth + weapon.defenseFactor(this));
	}

	@Override
	public boolean add(Buff buff) {
		if (super.add(buff)) {
			if (state == PASSIVE && buff.type == Buff.buffType.NEGATIVE) {
				state = HUNTING;
			}
			return true;
		}
		return false;
	}

	@Override
	public void damage( int dmg, Object src ) {

		if (state == PASSIVE) {
			state = HUNTING;
		}

		super.damage( dmg, src );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		damage = weapon.proc( this, enemy, damage );
		if (!enemy.isAlive() && enemy == Dungeon.hero){
			Dungeon.fail(this);
			GLog.n( Messages.capitalize(Messages.get(Char.class, "kill", name())) );
		}
		return damage;
	}

	@Override
	public void beckon( int cell ) {
		// Do nothing
	}

	@Override
	public void die( Object cause ) {
		weapon.identify(false);
		Dungeon.level.drop( weapon, pos ).sprite.drop();
		super.die( cause );
	}

	@Override
	public void destroy() {
		if (levelGenStatue && !CustomDungeon.isEditing()) {
			Notes.remove( Notes.Landmark.STATUE );
		}
		super.destroy();
	}

	@Override
	public float spawningWeight() {
		return 0f;
	}

	@Override
	public boolean reset() {
		state = PASSIVE;
		return true;
	}

	@Override
	public String description() {
		if (customDesc != null) return super.description();
		return Messages.get(this, "desc", weapon == null ? "___" : weapon().name());
	}

	{
		resistances.add(Grim.class);
	}

	public static Statue random(){
		return random( true );
	}

	public static Statue random( boolean useDecks ){
		Statue statue;
		if (Random.Int(10) == 0){
			statue = new ArmoredStatue();
		} else {
			statue = new Statue();
		}
		statue.createItems(useDecks);
		return statue;
	}

}