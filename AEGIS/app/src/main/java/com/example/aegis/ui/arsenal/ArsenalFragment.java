package com.example.aegis.ui.arsenal;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aegis.MainActivity;
import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.Armor;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.StateView;

import java.util.List;

/** Hall of Armor + Protocolo House Party. */
public class ArsenalFragment extends Fragment {

    private static final long PARTY_STEP_MS = 420;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private ArmorAdapter adapter;
    private RecyclerView carousel;
    private LinearLayoutManager layout;
    private StateView state;
    private TextView counter;
    private TextView banner;
    private View partyButton;
    private boolean partyRunning;
    private boolean loaded;
    private boolean partyWhenLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.fragment_arsenal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        state = view.findViewById(R.id.state);
        counter = view.findViewById(R.id.counter);
        banner = view.findViewById(R.id.banner);
        partyButton = view.findViewById(R.id.houseParty);
        carousel = view.findViewById(R.id.carousel);

        adapter = new ArmorAdapter(armor -> startActivity(ArmorDetailActivity.intent(requireContext(), armor.mark)));
        layout = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        carousel.setLayoutManager(layout);
        carousel.setAdapter(adapter);
        new LinearSnapHelper().attachToRecyclerView(carousel);
        carousel.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                updateCounter();
            }
        });

        partyButton.setOnClickListener(v -> startHouseParty());
        load();
    }

    private void load() {
        state.showLoading(getString(R.string.loading_targets));
        ApiClient.get().armors().enqueue(new ApiCallback<List<Armor>>() {
            @Override
            public void onSuccess(@NonNull List<Armor> armors) {
                if (!isAdded()) return;
                loaded = true;
                state.hide();
                adapter.submit(armors);
                updateCounter();
                if (partyWhenLoaded) {
                    partyWhenLoaded = false;
                    startHouseParty();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                state.showError(message, ArsenalFragment.this::load);
            }
        });
    }

    private void updateCounter() {
        int n = adapter.getItemCount();
        if (n == 0) return;
        int pos = Math.max(0, layout.findFirstCompletelyVisibleItemPosition());
        if (pos >= n) pos = n - 1;
        counter.setText(String.format("%02d / %02d", pos + 1, n));
    }

    /** Protocolo House Party: chama todas as armaduras, uma por uma. Também acionado pela J.A.R.V.I.S. */
    public void startHouseParty() {
        if (partyRunning) return;
        if (!loaded) {
            partyWhenLoaded = true; // a aba ainda está carregando: dispara assim que a lista chegar
            return;
        }
        partyRunning = true;
        partyButton.setEnabled(false);
        adapter.clearDeployed();
        Hud.typewriter(banner, "PROTOCOLO HOUSE PARTY INICIADO…");

        int total = adapter.getItemCount();
        for (int i = 0; i < total; i++) {
            final int position = i;
            handler.postDelayed(() -> {
                if (!isAdded()) return;
                carousel.smoothScrollToPosition(position);
                adapter.deploy(position);
                Hud.vibrate(requireContext(), 25);
            }, 900 + i * PARTY_STEP_MS);
        }
        handler.postDelayed(this::finishHouseParty, 900 + total * PARTY_STEP_MS + 300);
    }

    private void finishHouseParty() {
        if (!isAdded()) return;
        partyRunning = false;
        partyButton.setEnabled(true);
        Hud.typewriter(banner, getString(R.string.house_party_done));
        Hud.vibrate(requireContext(), 400);
        Hud.flash(((MainActivity) requireActivity()).flashOverlay(), 0x66FFB300);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        partyRunning = false;
        super.onDestroyView();
    }
}
