package com.example.aegis.ui.missions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.Mission;
import com.example.aegis.ui.hud.StateView;

import java.util.ArrayList;
import java.util.List;

/** Arquivos de missão: os grandes arcos de história (Guerra Civil, Guerra Infinita...). */
public class MissionsFragment extends Fragment {

    private final List<Mission> missions = new ArrayList<>();
    private StateView state;
    private RecyclerView list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.fragment_missions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        state = view.findViewById(R.id.state);
        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new MissionAdapter());
        load();
    }

    private void load() {
        state.showLoading(getString(R.string.missions_loading));
        ApiClient.get().missions().enqueue(new ApiCallback<List<Mission>>() {
            @Override
            public void onSuccess(@NonNull List<Mission> result) {
                if (!isAdded()) return;
                missions.clear();
                missions.addAll(result);
                list.getAdapter().notifyDataSetChanged();
                if (missions.isEmpty()) state.showEmpty(getString(R.string.empty_filter));
                else state.hide();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                state.showError(message, MissionsFragment.this::load);
            }
        });
    }

    private class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Mission m = missions.get(position);
            h.name.setText(m.name.toUpperCase());
            StringBuilder meta = new StringBuilder();
            if (m.issueCount != null) meta.append(m.issueCount).append(" EDIÇÕES");
            if (m.firstIssue != null) meta.append(meta.length() > 0 ? "  ·  " : "").append(m.firstIssue.toUpperCase());
            h.meta.setText(meta);
            h.deck.setText(m.deck != null ? m.deck : "");
            h.deck.setVisibility(m.deck != null ? View.VISIBLE : View.GONE);
            Glide.with(h.cover).load(m.imageUrl).into(h.cover);
            h.itemView.setOnClickListener(v ->
                    startActivity(MissionDetailActivity.intent(requireContext(), m.id)));
        }

        @Override
        public int getItemCount() {
            return missions.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView cover;
            final TextView name;
            final TextView meta;
            final TextView deck;

            Holder(View v) {
                super(v);
                cover = v.findViewById(R.id.cover);
                name = v.findViewById(R.id.name);
                meta = v.findViewById(R.id.meta);
                deck = v.findViewById(R.id.deck);
            }
        }
    }
}
