package com.example.hobbymasterytracker;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HobbyAdapter extends RecyclerView.Adapter<HobbyAdapter.HobbyViewHolder> {

    private static final int[] HOBBY_COLORS = {
        0xFF7DC52A, 0xFF1CB0F6, 0xFFFF9600,
        0xFFFF4B4B, 0xFFCE82FF, 0xFFFFC800,
    };

    private List<Hobby> hobbyList = new ArrayList<>();
    private OnHobbyClickListener onHobbyClickListener;
    private OnHobbyLongClickListener onHobbyLongClickListener;

    public Hobby getHobbyAt(int position) {
        return hobbyList.get(position);
    }

    public void setHobbies(List<Hobby> hobbies) {
        this.hobbyList = hobbies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HobbyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hobby, parent, false);
        return new HobbyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HobbyViewHolder holder, int position) {
        Hobby hobby = hobbyList.get(position);
        int color = (int) HOBBY_COLORS[position % HOBBY_COLORS.length];

        holder.txtName.setText(hobby.name);

        int hours = (int) hobby.totalHours;
        String hrsLabel = holder.itemView.getContext().getString(R.string.hrs);
        holder.txtHours.setText(String.format(Locale.getDefault(), "%,d / 10,000 %s", hours, hrsLabel));

        int level = (hours / 10) + 1;
        String levelLabel = holder.itemView.getContext().getString(R.string.level);
        holder.txtLevelBadge.setText(levelLabel + " " + level);

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.RECTANGLE);
        badgeBg.setCornerRadius(48f);
        badgeBg.setColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)));
        holder.txtLevelBadge.setBackground(badgeBg);
        holder.txtLevelBadge.setTextColor(color);

        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setShape(GradientDrawable.OVAL);
        iconBoxBg.setColor(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)));
        holder.iconBox.setBackground(iconBoxBg);

        holder.imgIcon.setText(getEmojiForHobby(hobby.name));

        float progress = (float) Math.min(hobby.totalHours / 10000.0, 1.0);
        holder.progressFill.post(() -> {
            int parentWidth = ((View) holder.progressFill.getParent()).getWidth();
            ViewGroup.LayoutParams lp = holder.progressFill.getLayoutParams();
            lp.width = (int) (parentWidth * progress);
            holder.progressFill.setLayoutParams(lp);
        });

        GradientDrawable fillBg = new GradientDrawable();
        fillBg.setShape(GradientDrawable.RECTANGLE);
        fillBg.setCornerRadius(24f);
        fillBg.setColor(color);
        holder.progressFill.setBackground(fillBg);

        holder.itemView.setOnClickListener(v -> {
            if (onHobbyClickListener != null) onHobbyClickListener.onHobbyClick(hobby);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onHobbyLongClickListener != null) onHobbyLongClickListener.onHobbyLongClick(hobby);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return hobbyList.size();
    }

    static class HobbyViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtHours, txtLevelBadge, imgIcon, txtMasterBadge;
        FrameLayout iconBox;
        View progressFill;

        public HobbyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtHours = itemView.findViewById(R.id.txtHours);
            txtLevelBadge = itemView.findViewById(R.id.txtLevelBadge);
            txtMasterBadge = itemView.findViewById(R.id.txtMasterBadge);
            iconBox = itemView.findViewById(R.id.iconBox);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            progressFill = itemView.findViewById(R.id.progressFill);
        }
    }

    public interface OnHobbyClickListener {
        void onHobbyClick(Hobby hobby);
    }

    public interface OnHobbyLongClickListener {
        void onHobbyLongClick(Hobby hobby);
    }

    public void setOnHobbyClickListener(OnHobbyClickListener listener) {
        this.onHobbyClickListener = listener;
    }

    public void setOnHobbyLongClickListener(OnHobbyLongClickListener listener) {
        this.onHobbyLongClickListener = listener;
    }

    private static String getEmojiForHobby(String name) {
        if (name == null) return "⭐";
        String lower = name.toLowerCase();
        if (lower.contains("piano") || lower.contains("music") || lower.contains("guitar")
                || lower.contains("violin") || lower.contains("drum") || lower.contains("sing"))
            return "🎵";
        if (lower.contains("code") || lower.contains("coding") || lower.contains("program")
                || lower.contains("software") || lower.contains("dev"))
            return "💻";
        if (lower.contains("paint") || lower.contains("draw") || lower.contains("art")
                || lower.contains("sketch"))
            return "🎨";
        if (lower.contains("gym") || lower.contains("fitness") || lower.contains("workout")
                || lower.contains("lift") || lower.contains("sport"))
            return "💪";
        if (lower.contains("read") || lower.contains("book"))
            return "📚";
        if (lower.contains("cook") || lower.contains("bake") || lower.contains("chef")
                || lower.contains("cooking"))
            return "🍳";
        if (lower.contains("run") || lower.contains("jog") || lower.contains("marathon"))
            return "🏃";
        if (lower.contains("swim"))
            return "🏊";
        if (lower.contains("chess"))
            return "♟️";
        if (lower.contains("photo") || lower.contains("camera"))
            return "📷";
        if (lower.contains("dance"))
            return "💃";
        if (lower.contains("yoga") || lower.contains("meditat"))
            return "🧘";
        if (lower.contains("game") || lower.contains("gaming"))
            return "🎮";
        if (lower.contains("write") || lower.contains("writing") || lower.contains("blog"))
            return "✍️";
        if (lower.contains("language") || lower.contains("spanish") || lower.contains("french")
                || lower.contains("japanese") || lower.contains("arabic"))
            return "🌍";
        if (lower.contains("football") || lower.contains("soccer"))
            return "⚽";
        if (lower.contains("basketball"))
            return "🏀";
        if (lower.contains("tennis"))
            return "🎾";
        return "⭐";
    }
}
