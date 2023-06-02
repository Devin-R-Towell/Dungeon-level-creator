package com.shatteredpixel.shatteredpixeldungeon.editor.overview;

import com.shatteredpixel.shatteredpixeldungeon.levels.editor.LevelScheme;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class WndSelectFloor extends Window {

    protected LevelListPane listPane;

    public WndSelectFloor() {
        resize(PixelScene.landscape() ? 215 : PixelScene.uiCamera.width - 5, (int) (PixelScene.uiCamera.height * 0.8f));

        listPane = new LevelListPane() {

            @Override
            public void onSelect(LevelScheme levelScheme,LevelListPane.ListItem listItem) {
                if (WndSelectFloor.this.onSelect(levelScheme)) hide();
            }

            @Override
            protected List<LevelScheme> filterLevels(Collection<LevelScheme> levels) {
                return WndSelectFloor.this.filterLevels(levels);
            }
        };
        add(listPane);

        listPane.setSize(width, height);
        PixelScene.align(listPane);

        listPane.updateList();

    }

    public abstract boolean onSelect(LevelScheme levelScheme);

    protected List<LevelScheme> filterLevels(Collection<LevelScheme> levels) {
        return new ArrayList<>(levels);
    }
}