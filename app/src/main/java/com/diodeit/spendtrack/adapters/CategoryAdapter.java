package com.diodeit.spendtrack.adapters;

import com.diodeit.spendtrack.utils.Tr;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.diodeit.spendtrack.R;
import com.diodeit.spendtrack.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private Context context;
    private List<Category> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public CategoryAdapter(Context context, List<Category> categories, OnCategoryClickListener listener) {
        this.context = context;
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvEmoji.setText(category.getEmoji());
        holder.tvName.setText(Tr.data(category.getShortName()));

        if (position == selectedPosition) {
            holder.itemView.setBackgroundResource(R.drawable.bg_pill_primary_container);
            holder.tvName.setTextColor(context.getColor(R.color.on_primary_container));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_pill_surface_container_low);
            holder.tvName.setTextColor(context.getColor(R.color.on_surface));
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_emoji);
            tvName = itemView.findViewById(R.id.tv_category_name);
        }
    }
}