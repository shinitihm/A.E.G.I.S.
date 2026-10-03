package com.example.aegis.ui.heroes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aegis.R;
import com.example.aegis.data.model.HeroSummary;
import com.example.aegis.data.model.Threat;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.ThreatMeterView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lista de alvos. Também é usada na aba de busca. */
public class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.Holder> {

    public interface OnHeroClick {
        void onClick(HeroSummary hero);
    }

    private final OnHeroClick onClick;
    private List<HeroSummary> items = new ArrayList<>();
    private Map<Integer, Threat> scanned;
    private Set<Integer> monitored = new HashSet<>();

    public HeroAdapter(OnHeroClick onClick) {
        this.onClick = onClick;
    }

    /** Ameaças escaneadas (do servidor + do aparelho) e alvos monitorados. */
    public void setLocalState(Map<Integer, Threat> scanned, Set<Integer> monitored) {
        this.scanned = scanned;
        this.monitored = monitored;
        notifyDataSetChanged();
    }

    public void submit(List<HeroSummary> list) {
        items = list;
        notifyDataSetChanged();
    }

    public Threat threatOf(HeroSummary hero) {
        if (hero.threat != null) return hero.threat;
        return scanned != null ? scanned.get(hero.id) : null;
    }

    public boolean isMonitored(HeroSummary hero) {
        return monitored.contains(hero.id);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hero, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        HeroSummary hero = items.get(position);
        h.name.setText(hero.name.toUpperCase());
        h.subtitle.setText(hero.realName != null ? hero.realName : h.itemView.getContext().getString(R.string.none_registered));
        h.monitored.setVisibility(isMonitored(hero) ? View.VISIBLE : View.GONE);
        Glide.with(h.thumb).load(hero.thumbUrl != null ? hero.thumbUrl : hero.imageUrl).into(h.thumb);

        Threat threat = threatOf(hero);
        if (threat != null) {
            int color = Hud.threatColor(h.itemView.getContext(), threat.levelCode);
            h.meter.setThreat(threat.score, color, false);
            h.threat.setText(threat.level + " · " + threat.score);
            h.threat.setTextColor(color);
        } else {
            h.meter.setThreat(0, h.itemView.getContext().getColor(R.color.aegis_cyan), false);
            h.threat.setText(R.string.not_scanned);
            h.threat.setTextColor(h.itemView.getContext().getColor(R.color.aegis_text_dim));
        }
        h.itemView.setOnClickListener(v -> onClick.onClick(hero));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final ImageView monitored;
        final TextView name;
        final TextView subtitle;
        final TextView threat;
        final ThreatMeterView meter;

        Holder(View v) {
            super(v);
            thumb = v.findViewById(R.id.thumb);
            monitored = v.findViewById(R.id.monitored);
            name = v.findViewById(R.id.name);
            subtitle = v.findViewById(R.id.subtitle);
            threat = v.findViewById(R.id.threat);
            meter = v.findViewById(R.id.meter);
        }
    }
}
