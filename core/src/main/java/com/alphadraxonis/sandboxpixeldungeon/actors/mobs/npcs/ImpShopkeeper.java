/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
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

package com.alphadraxonis.sandboxpixeldungeon.actors.mobs.npcs;

import com.alphadraxonis.sandboxpixeldungeon.Dungeon;
import com.alphadraxonis.sandboxpixeldungeon.actors.buffs.AscensionChallenge;
import com.alphadraxonis.sandboxpixeldungeon.editor.levels.CustomDungeon;
import com.alphadraxonis.sandboxpixeldungeon.messages.Messages;
import com.alphadraxonis.sandboxpixeldungeon.sprites.ImpSprite;

public class ImpShopkeeper extends Shopkeeper {

	{
		spriteClass = ImpSprite.class;
	}
	
	private boolean seenBefore = false;
	
	@Override
	protected boolean act() {

		if (!seenBefore && Dungeon.level.heroFOV[pos]) {
			if (Dungeon.hero.buff(AscensionChallenge.class) == null) {
				yell(Messages.get(this, "greetings", Messages.titleCase(Dungeon.hero.name())));
			} else {
				yell(Messages.get(this, "greetings_ascent", Messages.titleCase(Dungeon.hero.name())));
			}
			seenBefore = true;
		}

		return super.act();
	}

	@Override
	public String name() {
		return super.name() + (CustomDungeon.isEditing() ? " (" + Messages.get(Shopkeeper.class, "name") + ")" : "");
	}
}