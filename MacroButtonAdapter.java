package com.btmacromouse;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MacroButtonAdapter extends RecyclerView.Adapter<MacroButtonAdapter.ViewHolder> {

    public interface OnButtonClickListener {
        void onMacroClick(int position);
        void onEditClick(int position);
        void onDeleteClick(int position);
    }

    private final List<MacroButton> buttons;
    private final OnButtonClickListener listener;
    private boolean connected = false;

    public MacroButtonAdapter(List<MacroButton> buttons, OnButtonClickListener listener) {
        this.buttons = buttons;
        this.listener = listener;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_macro_button, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MacroButton btn = buttons.get(position);

        holder.btnName.setText(btn.getName());
        holder.btnCoords.setText("X: " + btn.getTargetX() + "  Y: " + btn.getTargetY());

        // Set button background color
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(24f);
        bg.setColor(btn.getColor());
        holder.macroButton.setBackground(bg);
        holder.macroButton.setAlpha(connected ? 1.0f : 0.5f);
        holder.macroButton.setEnabled(connected);

        holder.macroButton.setOnClickListener(v -> listener.onMacroClick(holder.getAdapterPosition()));
        holder.editBtn.setOnClickListener(v -> listener.onEditClick(holder.getAdapterPosition()));
        holder.deleteBtn.setOnClickListener(v -> listener.onDeleteClick(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return buttons.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView btnName, btnCoords;
        View macroButton;
        ImageButton editBtn, deleteBtn;

        ViewHolder(View v) {
            super(v);
            btnName = v.findViewById(R.id.btn_name);
            btnCoords = v.findViewById(R.id.btn_coords);
            macroButton = v.findViewById(R.id.macro_button);
            editBtn = v.findViewById(R.id.edit_btn);
            deleteBtn = v.findViewById(R.id.delete_btn);
        }
    }
}
