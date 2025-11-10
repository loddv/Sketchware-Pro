package mod.loddv.dev.designer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import pro.sketchware.R;

public class GridOptionAdapter extends RecyclerView.Adapter<GridOptionAdapter.ViewHolder> {

	private final List<Option> options;

	public GridOptionAdapter(List<Option> options) {
		this.options = options;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
				            .inflate(R.layout.property_grid_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Option opt = options.get(position);
		holder.icon.setImageResource(opt.iconRes);
		holder.title.setText(opt.title);
		holder.itemView.setOnClickListener(opt.listener);
		holder.itemView.setId(opt.id);
		holder.bindVisibility(opt.isVisible);
	}

	@Override
	public int getItemCount() {
		return options.size();
	}

	public void setItemVisibility(int viewId, boolean isVisible) {
		for (int i = 0; i < options.size(); i++) {
			Option option = options.get(i);
			if (option.id == viewId) {
				option.isVisible = isVisible;
				notifyItemChanged(i);
				// Assuming viewId is unique, we can break here
				break;
			}
		}
	}

	public static class Option {
		String title;
		int iconRes;
		View.OnClickListener listener;
		int id = View.NO_ID;
		boolean isVisible = true;

		public Option(String title, int iconRes, View.OnClickListener listener) {
			this.title = title;
			this.iconRes = iconRes;
			this.listener = listener;
		}

		public Option setId(int id) {
			this.id = id;
			return this;
		}
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		final int originalWidth;
		final int originalHeight;
		final ViewGroup.MarginLayoutParams originalMargins;
		ImageView icon;
		TextView title;

		ViewHolder(final View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.img_icon);
			title = itemView.findViewById(R.id.tv_title);

			ViewGroup.LayoutParams lp = itemView.getLayoutParams() != null ? itemView.getLayoutParams() : new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

			originalWidth = lp.width;
			originalHeight = lp.height;
			originalMargins = lp instanceof ViewGroup.MarginLayoutParams ? (ViewGroup.MarginLayoutParams) lp : new ViewGroup.MarginLayoutParams(0, 0);

			((ViewGroup) itemView).setForegroundGravity(17 | 48);
			// Not exactly the best place to do this, but it works. The gravity of the LinearLayout
			// (the root of property_grid_item.xml) has to be changed, otherwise the TextView gets
			// cut off as it's centered vertically.
			// Fun fact: old versions of Sketchware used to use this same dirty trick.
			title.post(new Runnable() {
				@Override
				public void run() {
					ViewGroup.LayoutParams layoutParams = title.getLayoutParams();
					layoutParams.width = itemView.getWidth() - 8;
					title.setLayoutParams(layoutParams);
				}
			});

		}

		void bindVisibility(boolean isVisible) {
			if (isVisible) {
				itemView.setVisibility(View.VISIBLE);
				RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(originalWidth, originalHeight);
				params.setMargins(originalMargins.leftMargin, originalMargins.topMargin, originalMargins.rightMargin, originalMargins.bottomMargin);
				itemView.setLayoutParams(params);
			} else {
				itemView.setVisibility(View.GONE);
				itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
			}
		}
	}
}