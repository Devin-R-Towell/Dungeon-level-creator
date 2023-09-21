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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.CaveRoom;
import com.watabou.utils.Point;

public class MineGiantRoom extends CaveRoom {

	@Override
	public float[] sizeCatProbs() {
		return new float[]{0, 0, 1};
	}

	@Override
	protected float fill() {
		return 0.70f;
	}

	@Override
	public void paint(Level level) {
		super.paint(level);

		if (Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL){
			Painter.fillEllipse(level, this, 3, Terrain.MINE_CRYSTAL);
			Painter.fillEllipse(level, this, 5, Terrain.EMPTY);

			for (int i = 0; i < width()*height()/4; i ++){
				Point r = random(1);
				if (level.map[level.pointToCell(r)] != Terrain.WALL) {
					Painter.set(level, r, Terrain.MINE_CRYSTAL);
				}
			}

			Point p = center();
			CrystalSpire m = new CrystalSpire();
			m.pos = level.pointToCell(p);
			level.mobs.add(m);
			Painter.set(level, p, Terrain.CUSTOM_DECO_EMPTY);
			Painter.set(level, p.x-1, p.y, Terrain.CUSTOM_DECO_EMPTY);
			Painter.set(level, p.x+1, p.y, Terrain.CUSTOM_DECO_EMPTY);
			Painter.set(level, p.x, p.y-1, Terrain.CUSTOM_DECO_EMPTY);
			Painter.set(level, p.x, p.y-2, Terrain.CUSTOM_DECO_EMPTY);

		} else {
			Painter.fillEllipse(level, this, 3, Terrain.EMPTY);
		}

	}

}
