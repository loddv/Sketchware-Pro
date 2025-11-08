package pro.sketchware.activities.preview;

import android.os.Bundle;
import android.view.MotionEvent;

import com.besome.sketch.beans.ViewBean;
import com.besome.sketch.editor.view.ItemView;
import com.besome.sketch.editor.view.ViewPane;
import com.besome.sketch.lib.base.BaseAppCompatActivity;

import java.util.ArrayList;

import a.a.a.jC;
import a.a.a.mB;
import pro.sketchware.databinding.ActivityLayoutPreviewBinding;
import pro.sketchware.tools.ViewBeanParser;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

public class LayoutPreviewActivity extends BaseAppCompatActivity {

	private ViewPane pane;

	private String content;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		enableEdgeToEdgeNoContrast();
		super.onCreate(savedInstanceState);
		ActivityLayoutPreviewBinding binding = ActivityLayoutPreviewBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		//var toolbar = binding.toolbar;
		var fab = binding.fab;
		//setSupportActionBar(toolbar);
		//getSupportActionBar().setTitle("Layout Preview");
		//getSupportActionBar().setSubtitle(getIntent().getStringExtra("title"));
		//getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		//getSupportActionBar().setDisplayShowTitleEnabled(true);
		fab.setOnClickListener(v -> {
			if (! mB.a()) {
				onBackPressed();
			}
		});
		fab.setOnLongClickListener(
				v -> {
					if (fab.getAlpha() == 1.0f) {
						fab.setAlpha(0.2f);
					} else {
						fab.setAlpha(1.0f);
					}
					return true;
				}
		);
		fab.setOnHoverListener((v, event) -> {
			switch (event.getAction()) {
				case MotionEvent.ACTION_HOVER_MOVE:
					fab.setAlpha(0.2f);
					break;
				case android.view.MotionEvent.ACTION_HOVER_EXIT:
					fab.setAlpha(1.0f);
					break;
			}
			return false;
		});
		content = getIntent().getStringExtra("xml");
		var sc_id = getIntent().getStringExtra("sc_id");
		pane = binding.pane;
		pane.initialize(sc_id, true);
		pane.updateRootLayout(sc_id, getIntent().getStringExtra("title"));
		pane.setVerticalScrollBarEnabled(true);
		pane.setResourceManager(jC.d(sc_id));
		UI.addSystemWindowInsetToPadding(binding.pane, false, false, false, true);
	}

	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if (content != null) {
			try {
				var parser = new ViewBeanParser(content);
				loadViews(parser.parse());
			} catch (Exception e) {
				SketchwareUtil.toastError(e.toString());
			}
		} else {
			SketchwareUtil.toastError("content is null");
		}
	}

	private ItemView loadView(ViewBean view) {
		var itemView = pane.createItemView(view);
		pane.addViewAndUpdateIndex(itemView);
		if (itemView instanceof ItemView sy) {
			sy.setFixed(true);
			return sy;
		}
		return null;
	}

	private ItemView loadViews(ArrayList<ViewBean> views) {
		ItemView itemView = null;
		for (ViewBean view : views) {
			if (views.indexOf(view) == 0) {
				view.parent = "root";
				view.parentType = 0;
				view.preParent = null;
				view.preParentType = - 1;
				itemView = loadView(view);
			} else {
				loadView(view);
			}
		}
		return itemView;
	}
}
