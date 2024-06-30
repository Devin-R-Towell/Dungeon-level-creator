package com.shatteredpixel.shatteredpixeldungeon.editor.server;

import com.shatteredpixel.shatteredpixeldungeon.editor.overview.dungeon.WndSelectDungeon;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.MultiWindowTabComp;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.spinner.Spinner;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.spinner.SpinnerIntegerModel;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilies;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.services.server.DungeonPreview;
import com.shatteredpixel.shatteredpixeldungeon.services.server.ServerCommunication;
import com.shatteredpixel.shatteredpixeldungeon.services.updates.Updates;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.HashSet;
import java.util.Set;

public class ServerDungeonList extends MultiWindowTabComp {

	private static ServerDungeonList instance;

	public static DungeonPreview[][] dungeons;

	private Spinner outsideSp;
	protected RedButton upload;
	protected RedButton refresh;
	protected RenderedTextBlock msgWaitingForData;

	private int page = 0;
	private static int numPages, lastPage = 0;
	public static final int PREVIEWS_PER_PAGE = 4;

	private static final Set<Integer> pagesLoading = new HashSet<>(5);

	public ServerDungeonList() {

		instance = this;

		mainWindowComps = new Component[PREVIEWS_PER_PAGE];

		upload = new RedButton("") {
			@Override
			protected void onClick() {
				UploadDungeon.showUploadWindow(ServerCommunication.UploadType.UPLOAD, null);
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndSelectDungeon.class, "upload_label");
			}
		};
		upload.icon(Icons.UPLOAD.get());
		add(upload);

		refresh = new RedButton("") {
			@Override
			protected void onClick() {
				dungeons = null;
				hidePage(page);
				requestPage(lastPage, true);
				for (int i = Math.max(0, lastPage-1); i < 3; i++) {
					loadPageFromServer(i, false);
				}
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndSelectDungeon.class, "refresh_label");
			}
		};
		refresh.icon(Icons.REFRESH.get());
		refresh.icon().scale.set(0.7f);
		add(refresh);

		title = createTitle();
		add(title);

		msgWaitingForData = PixelScene.renderTextBlock(Messages.get(ServerCommunication.class, "wait_body"), 6);
		content.add(msgWaitingForData);

		outsideSp = new Spinner(new SpinnerIntegerModel(1, numPages, lastPage+1, true) {
			{
				setAbsoluteMinAndMax(getMinimum(), getMaximum());
			}

			@Override
			protected String displayString(Object value) {
				return Messages.get(ServerDungeonList.class, "page", outsideSp == null ? lastPage+1 : value, numPages);
			}

			@Override
			public float getInputFieldWidth(float height) {
				return Spinner.FILL;
			}
		}, "", 10) {
			@Override
			protected void layout() {
				height = 20;
				super.layout();
			}
		};
		outsideSp.visible = outsideSp.active = numPages > 1;

		outsideSp.addChangeListener(() -> {
			hidePage(page);
			requestPage((int) outsideSp.getValue()-1, true);
			layout();
			sp.scrollTo(0, 0);
		});

		if (dungeons == null) {
			for (int i = Math.max(0, lastPage-1); i < 3; i++) {
				loadPageFromServer(i, false);
			}
		}
		requestPage(lastPage, true);
	}

	public static void setNumPreviews(int numPreviews) {
		numPages = (int) Math.ceil((float) numPreviews / PREVIEWS_PER_PAGE);
		if (dungeons == null) {
			dungeons = new DungeonPreview[numPages][];
		}
		if (instance != null) {
			instance.outsideSp.visible = instance.outsideSp.active = numPages > 1;
			((SpinnerIntegerModel) instance.outsideSp.getModel()).setMaximum(numPages);
			((SpinnerIntegerModel) instance.outsideSp.getModel()).setAbsoluteMaximum(numPages);
			Game.runOnRenderThread(() -> instance.outsideSp.setValue(instance.outsideSp.getValue()));
			((WndServerDungeonList) EditorUtilies.getParentWindow(instance)).updateLayout();
		}
	}

	private void hidePage(int page) {
		for (int i = 0; i < mainWindowComps.length; i++) {
			if (mainWindowComps[i] != null) {
				mainWindowComps[i].remove();
				mainWindowComps[i].killAndErase();
				mainWindowComps[i].destroy();
				mainWindowComps[i] = null;
			}
		}
	}

	private void initPage(int page) {
		msgWaitingForData.setVisible(false);
		this.page = page;
		for (int i = 0; i < dungeons[page].length; i++) {
			mainWindowComps[i] = createListItem(dungeons[page][i]);
			content.add(mainWindowComps[i]);
		}
	}

	public static void updatePage() {
		if (instance != null) {
			instance.hidePage(instance.page);
			instance.initPage(instance.page);
		}
	}

	public void requestPage(int page, boolean startMoreProcesses) {
		lastPage = page;
		if (dungeons == null || dungeons.length > 0 && dungeons[page] == null) {
			msgWaitingForData.setVisible(true);
			loadPageFromServer(page, startMoreProcesses);
		} else {
			if (dungeons.length == 0) {
				msgWaitingForData.setVisible(false);
				if (sp.camera() != null) {
					layout();
					sp.scrollTo(0, 0);
				}
			}
			else initPage(page);
		}
	}

	private void loadPageFromServer(final int page, boolean startMoreProcesses) {
		if (pagesLoading.contains(page)) return;
		pagesLoading.add(page);
		ServerCommunication.dungeonList(new ServerCommunication.OnPreviewReceive() {
			@Override
			protected void onSuccessful(DungeonPreview[] previews) {
				pagesLoading.remove(page);
				if (dungeons.length <= page) {
					if (dungeons.length == 0) {
						msgWaitingForData.setVisible(false);
						instance.layout();
						instance.sp.scrollTo(0, 0);
					}
					return;
				}
				dungeons[page] = previews;
				if (lastPage == page && instance != null) {
					instance.initPage(page);
					instance.layout();
					instance.sp.scrollTo(0, 0);
				}
				if (startMoreProcesses && instance != null) {
					if (page + 1 < dungeons.length && dungeons[page + 1] == null && !pagesLoading.contains(page+1)) loadPageFromServer(page+1, true);
					else if (page > 0 && dungeons[page - 1] != null  && !pagesLoading.contains(page-1)) loadPageFromServer(page-1, true);
				}
			}
		}, page);
	}

	@Override
	protected void layoutOwnContent() {
		if (mainWindowComps[0] == null) {
			msgWaitingForData.maxWidth((int) width);
			msgWaitingForData.setPos((width - msgWaitingForData.width()) * 0.5f, 0);
			content.setSize(msgWaitingForData.width(), msgWaitingForData.height());
		}
		else {
			content.setSize(width, 0);
			content.setSize(width, EditorUtilies.layoutCompsLinear(GAP, content, mainWindowComps));
		}
		upload.setRect(x + width - 17, y + height - 15, 16, 16);
	}

	@Override
	public void changeContent(Component titleBar, Component body, Component outsideSp, float contentAlignmentV, float titleAlignmentH) {
		upload.setVisible(false);
		refresh.setVisible(false);
		super.changeContent(titleBar, body, outsideSp, contentAlignmentV, titleAlignmentH);
	}

	@Override
	public void closeCurrentSubMenu() {
		upload.setVisible(true);
		refresh.setVisible(true);
		super.closeCurrentSubMenu();
	}

	public Component createTitle() {
		return new Component() {
			private IconTitle main;
			{
				main = new IconTitle(null, Messages.titleCase(Messages.get(ServerDungeonList.class, "title")));
				add(main);
			}

			@Override
			protected void layout() {
				main.setRect(x, y, width - 20, -1);
				height = Math.max(18, main.height());
				refresh.setRect(width - 17, (height - 16) * 0.5f + 1, 16, 16);
			}
		};
	};

	public Component getOutsideSp() {
		return outsideSp;
	}

	@Override
	public Image createIcon() {
		return null;
	}

	@Override
	public String hoverText() {
		return null;
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		if (instance == this) instance = null;
	}

	private DungeonPreviewItem createListItem(DungeonPreview preview) {
		return preview.dungeonFileID.startsWith("ERROR")
				? new DungeonPreviewItem(preview.dungeonFileID)
				: new DungeonPreviewItem(preview);
	}

	private class DungeonPreviewItem extends ScrollingListPane.ListItem {

		private final DungeonPreview preview;

		protected RenderedTextBlock desc;

		protected RenderedTextBlock label2, label3;

		private DungeonPreviewItem(String error) {
			super(new Image(), Messages.get(ServerCommunication.class, "error") + ": " + error);
			preview = null;
		}

		private DungeonPreviewItem(DungeonPreview preview) {
			super(new Image(), null, preview.title);
			this.preview = preview;

			desc.text(preview.description);

			label2.text(Messages.get(ServerDungeonList.class, "title_entry"));
			label3.text("_" + preview.uploader + "_");
		}

		@Override
		protected void createChildren(Object... params) {
			super.createChildren(params);

			label.setHighlighting(false);
			label.hardlight(Window.TITLE_COLOR);

			label2 = PixelScene.renderTextBlock(7);
			label2.setHighlighting(false);
			add(label2);

			label3 = PixelScene.renderTextBlock(7);
			add(label3);

			desc = PixelScene.renderTextBlock(6);
			desc.maxNumLines = 3;
			add(desc);
		}

		@Override
		protected void layout() {
			if (desc == null) {
				super.layout();
				return;
			}

			desc.maxWidth(getLabelMaxWidth() - 3);
			label.maxWidth(getLabelMaxWidth());
			height = label.height() + desc.height() + 9;
			super.layout();
			label.setPos(label.left(), y + 3);

			label2.setPos(label.right(), label.top());
			label3.setPos(label2.right(), label2.top());
			
			if (label3.right() > width - 3) {
				if (label3.width() + label2.width() < label.maxWidth()) {
					label2.setPos(label.left(), label.bottom() + 4);
					label3.setPos(label2.right(), label2.top());
				} else if (label2.right() <= width - 3) {
					label3.setPos(label.left(), label.bottom() + 4);
				} else {
					label2.setPos(label.left(), label.bottom() + 4);
					label3.setPos(label2.left(), label2.bottom() + 4);
				}
			}

			if (desc != null) desc.setPos(label.left() + 3, label.bottom() + 4);
		}

		@Override
		protected void onClick() {
			if (preview != null) {
				WndPreview prev = new WndPreview(preview, ServerDungeonList.this);
				changeContent(prev.createTitle(), prev, prev.getOutsideSp());

				if (preview.intVersion > Game.versionCode) {
					Game.scene().addToFront(new WndOptions(
							Icons.WARNING.get(),
							Messages.titleCase(Messages.get(WndPreview.class, "update_req_title")),
							Messages.get(WndPreview.class, "update_req_body"),
							Messages.get(WndPreview.class, "update_now"),
							Messages.get(WndPreview.class, "update_later")
					) {
						@Override
						protected void onSelect(int index) {
							if (index == 0) {
								if (Updates.updateAvailable()) Updates.launchUpdate(Updates.updateData());
								else Game.platform.openURI("https://github.com/AlphaDraxonis/SandboxPixelDungeon/releases/latest");
							}
						}
					});
				}
			}
		}
	}

	public static void updateLayout() {
		if (instance != null) instance.layout();
	}

	public static class WndServerDungeonList extends Window {
		private ServerDungeonList serverDungeonList;
		private Component outsideSp;

		public WndServerDungeonList() {
			super(Math.min(WndTitledMessage.WIDTH_MAX, (int) (PixelScene.uiCamera.width * 0.9)), (int) (PixelScene.uiCamera.height * 0.8f));

			add(serverDungeonList = new ServerDungeonList() {
				@Override
				public void changeContent(Component titleBar, Component body, Component outsideSp, float contentAlignmentV, float titleAlignmentH) {
					super.changeContent(titleBar, body, outsideSp, contentAlignmentV, titleAlignmentH);
					if (WndServerDungeonList.this.outsideSp != null) {
						WndServerDungeonList.this.outsideSp.setVisible(false);
						serverDungeonList.setSize(WndServerDungeonList.this.width, WndServerDungeonList.this.height - 2);
					}
					sp.givePointerPriority();
					upload.givePointerPriority();
				}

				@Override
				public void closeCurrentSubMenu() {
					super.closeCurrentSubMenu();
					if (outsideSp != null) {
						outsideSp.setVisible(numPages > 1);
						if (outsideSp.visible)
							serverDungeonList.setSize(WndServerDungeonList.this.width, WndServerDungeonList.this.height - outsideSp.height() - 4);
					}
				}
			});
			outsideSp = serverDungeonList.getOutsideSp();
			add(outsideSp);

			updateLayout();
		}

		private void updateLayout() {
			if (outsideSp != null && outsideSp.visible) {
				outsideSp.setSize(width, -1);
				outsideSp.setPos(0, height - outsideSp.height());
				serverDungeonList.setRect(0, 2, width, height - outsideSp.height() - 4);
			} else {
				serverDungeonList.setRect(0, 2, width, height - 2);
			}
			serverDungeonList.sp.givePointerPriority();
			serverDungeonList.upload.givePointerPriority();
		}
	}

}