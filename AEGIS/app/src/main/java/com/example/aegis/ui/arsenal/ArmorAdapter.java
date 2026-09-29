package com.example.aegis.ui.arsenal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aegis.R;
import com.example.aegis.data.model.Armor;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.IronHelmetView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Carrossel do Hall of Armor: cada armadura num pedestal, colorida com as cores da Mark. */
public class ArmorAdapter extends RecyclerView.Adapter<ArmorAdapter.Holder> {

    public interface OnArmorClick {
        void onClick(Armor armor);
    }

    private final OnArmorClick onClick;
    private final Set<Integer> deployed = new HashSet<>();
    private List<Armor> items = new ArrayList<>();

    public ArmorAdapter(OnArmorClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<Armor> list) {
        items = list;
        notifyDataSetChanged();
    }

    public Armor get(int position) {
        return items.get(position);
    }

    public void deploy(int position) {
        if (deployed.add(items.get(position).mark)) notifyItemChanged(position);
    }

    public void clearDeployed() {
        deployed.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_armor, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Armor a = items.get(position);
        h.code.setText(a.code);
        h.nickname.setText(a.nickname != null ? a.nickname.toUpperCase() : "");
        h.debut.setText(a.debut);
        h.status.setText(a.status);
        h.status.setTextColor(Hud.statusColor(h.itemView.getContext(), a.status));
        h.helmet.setColors(Hud.parseColor(a.primaryColor, 0xFFB71C1C), Hud.parseColor(a.secondaryColor, 0xFFFFB300));
        h.deployed.setVisibility(deployed.contains(a.mark) ? View.VISIBLE : View.INVISIBLE);
        h.itemView.setOnClickListener(v -> onClick.onClick(a));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView code;
        final TextView nickname;
        final TextView status;
        final TextView debut;
        final TextView deployed;
        final IronHelmetView helmet;

        Holder(View v) {
            super(v);
            code = v.findViewById(R.id.code);
            nickname = v.findViewById(R.id.nickname);
            status = v.findViewById(R.id.status);
            debut = v.findViewById(R.id.debut);
            deployed = v.findViewById(R.id.deployed);
            helmet = v.findViewById(R.id.helmet);
        }
    }
}
