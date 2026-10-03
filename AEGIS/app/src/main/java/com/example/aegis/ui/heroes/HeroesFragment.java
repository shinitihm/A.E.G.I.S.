package com.example.aegis.ui.heroes;

import android.content.Intent;
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
import com.example.aegis.data.LocalStore;
import com.example.aegis.data.model.HeroDetail;
import com.example.aegis.data.model.HeroPage;
import com.example.aegis.data.model.HeroSummary;
import com.example.aegis.data.model.Threat;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.StateView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Banco de dados de heróis: lista paginada, filtros, alvo do dia e atalho para o comparador. */
public class HeroesFragment extends Fragment {

    private final List<HeroSummary> all = new ArrayList<>();
    private HeroAdapter adapter;
    private LocalStore store;
    private StateView state;
    private ChipGroup filters;
    private View dayCard;
    private RecyclerView list;
    private boolean loading;
    private Integer nextPage = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.fragment_heroes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        store = new LocalStore(requireContext());
        state = view.findViewById(R.id.state);
        filters = view.findViewById(R.id.filters);
        dayCard = view.findViewById(R.id.dayCard);

        adapter = new HeroAdapter(hero -> startActivity(HeroDetailActivity.intent(requireContext(), hero.id)));
        list = view.findViewById(R.id.list);
        LinearLayoutManager layout = new LinearLayoutManager(requireContext());
        list.setLayoutManager(layout);
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 0 && layout.findLastVisibleItemPosition() >= adapter.getItemCount() - 4) loadNext();
            }
        });

        filters.setOnCheckedStateChangeListener((group, ids) -> applyFilter());
        view.findViewById(R.id.compare).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CompareActivity.class)));

        loadNext();
        loadTargetOfTheDay(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) applyFilter(); // pode ter escaneado ou monitorado alguém na ficha
    }

    /** Easter egg "snap": metade dos cards visíveis vira pó e volta sozinha alguns segundos depois. */
    public void snap() {
        if (list == null) return;
        list.post(() -> { // post: a aba pode ter acabado de ser mostrada e ainda não ter os cards na tela
            list.suppressLayout(true); // sem rolagem nem reciclagem enquanto há cards invisíveis
            for (int i = 0; i < list.getChildCount(); i += 2) {
                View card = list.getChildAt(i);
                card.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f)
                        .translationX(card.getWidth() * 0.35f).translationY(-card.getHeight() * 0.4f)
                        .setStartDelay(i * 70L).setDuration(900)
                        .withEndAction(() -> card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                                .translationX(0f).translationY(0f)
                                .setStartDelay(2500).setDuration(600)
                                // o atraso fica guardado na view: zera para não atrasar as animações da lista
                                .withEndAction(() -> card.animate().setStartDelay(0))
                                .start())
                        .start();
            }
            list.postDelayed(() -> list.suppressLayout(false), 5500);
        });
    }

    private void loadNext() {
        if (loading || nextPage == null) return;
        loading = true;
        if (all.isEmpty()) state.showLoading(getString(R.string.loading_targets));

        ApiClient.get().heroes(nextPage).enqueue(new ApiCallback<HeroPage>() {
            @Override
            public void onSuccess(@NonNull HeroPage page) {
                loading = false;
                if (!isAdded()) return;
                all.addAll(page.items);
                nextPage = page.nextPage;
                state.hide();
                applyFilter();
                // a Comic Vine mistura editoras: se a página veio quase vazia de Marvel, busca a próxima
                if (page.items.size() < 8) loadNext();
            }

            @Override
            public void onError(@NonNull String message) {
                loading = false;
                if (!isAdded()) return;
                if (all.isEmpty()) state.showError(message, HeroesFragment.this::loadNext);
            }
        });
    }

    private void loadTargetOfTheDay(View root) {
        ApiClient.get().targetOfTheDay().enqueue(new ApiCallback<HeroDetail>() {
            @Override
            public void onSuccess(@NonNull HeroDetail hero) {
                if (!isAdded()) return;
                store.saveThreat(hero.id, hero.threat);
                ((TextView) root.findViewById(R.id.dayName)).setText(hero.name.toUpperCase());
                TextView threat = root.findViewById(R.id.dayThreat);
                threat.setText(hero.threat.level + " · " + hero.threat.score);
                threat.setTextColor(Hud.threatColor(requireContext(), hero.threat.levelCode));
                Glide.with(HeroesFragment.this).load(hero.thumbUrl).into((ImageView) root.findViewById(R.id.dayThumb));
                dayCard.setOnClickListener(v -> startActivity(HeroDetailActivity.intent(requireContext(), hero.id)));
                dayCard.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(@NonNull String message) {
                // o alvo do dia é opcional: sem ele a tela funciona normalmente
            }
        });
    }

    private void applyFilter() {
        Map<Integer, Threat> scanned = store.threats();
        Set<Integer> monitored = new HashSet<>();
        for (HeroSummary h : store.monitored()) monitored.add(h.id);
        adapter.setLocalState(scanned, monitored);

        int checked = filters.getCheckedChipId();
        List<HeroSummary> shown = new ArrayList<>();
        if (checked == R.id.filterMonitored) {
            // monitorados podem vir de buscas e nem estar na lista principal
            shown.addAll(store.monitored());
        } else {
            for (HeroSummary h : all) {
                Threat t = adapter.threatOf(h);
                if (checked == R.id.filterScanned && t == null) continue;
                if (checked == R.id.filterOmega && (t == null || !"OMEGA".equals(t.levelCode))) continue;
                shown.add(h);
            }
        }
        adapter.submit(shown);
        if (shown.isEmpty() && !all.isEmpty()) state.showEmpty(getString(R.string.empty_filter));
        else if (!all.isEmpty()) state.hide();
    }
}
