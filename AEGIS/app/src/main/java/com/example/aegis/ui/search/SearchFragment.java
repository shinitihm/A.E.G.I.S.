package com.example.aegis.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.LocalStore;
import com.example.aegis.data.model.HeroSummary;
import com.example.aegis.ui.heroes.HeroAdapter;
import com.example.aegis.ui.heroes.HeroDetailActivity;
import com.example.aegis.ui.hud.StateView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;

/** Rastreia novos alvos na Comic Vine: busca com debounce e cancelamento da requisição anterior. */
public class SearchFragment extends Fragment {

    private static final long DEBOUNCE_MS = 450;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private HeroAdapter adapter;
    private LocalStore store;
    private StateView state;
    private EditText query;
    private Call<List<HeroSummary>> running;
    private Runnable pending;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        store = new LocalStore(requireContext());
        state = view.findViewById(R.id.state);
        query = view.findViewById(R.id.query);

        adapter = new HeroAdapter(hero -> startActivity(HeroDetailActivity.intent(requireContext(), hero.id)));
        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        state.showEmpty(getString(R.string.search_idle));

        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                schedule(s.toString().trim());
            }
        });
        query.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                schedule(query.getText().toString().trim(), 0);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) refreshLocalState();
    }

    private void schedule(String text) {
        schedule(text, DEBOUNCE_MS);
    }

    private void schedule(String text, long delay) {
        if (pending != null) handler.removeCallbacks(pending);
        if (running != null) running.cancel();
        if (text.length() < 2) {
            adapter.submit(java.util.Collections.emptyList());
            state.showEmpty(getString(R.string.search_idle));
            return;
        }
        pending = () -> search(text);
        handler.postDelayed(pending, delay);
    }

    private void search(String text) {
        state.showLoading(getString(R.string.search_tracking));
        adapter.submit(java.util.Collections.emptyList());
        running = ApiClient.get().search(text);
        running.enqueue(new ApiCallback<List<HeroSummary>>() {
            @Override
            public void onSuccess(@NonNull List<HeroSummary> results) {
                if (!isAdded()) return;
                refreshLocalState();
                adapter.submit(results);
                if (results.isEmpty()) state.showEmpty(getString(R.string.search_empty));
                else state.hide();
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                state.showError(message, () -> search(text));
            }
        });
    }

    private void refreshLocalState() {
        Set<Integer> monitored = new HashSet<>();
        for (HeroSummary h : store.monitored()) monitored.add(h.id);
        adapter.setLocalState(store.threats(), monitored);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        if (running != null) running.cancel();
        super.onDestroyView();
    }
}
