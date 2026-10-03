package com.example.aegis.ui.briefing;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.aegis.R;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.LocalStore;
import com.example.aegis.data.model.Briefing;
import com.example.aegis.ui.hud.StateView;
import com.google.android.material.button.MaterialButton;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

/** Pop-up ao abrir o app: pergunta se o usuário quer o briefing (clima, última chuva e 2 manchetes). */
public class BriefingDialogFragment extends DialogFragment {

    public static final String TAG = "briefing";
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private LocalStore store;
    private View askGroup;
    private View resultGroup;
    private EditText cityInput;
    private MaterialButton yes;
    private StateView state;
    private Call<Briefing> call;

    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        setStyle(STYLE_NO_TITLE, R.style.Aegis_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.dialog_briefing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        store = new LocalStore(requireContext());
        askGroup = view.findViewById(R.id.askGroup);
        resultGroup = view.findViewById(R.id.resultGroup);
        cityInput = view.findViewById(R.id.city);
        yes = view.findViewById(R.id.yes);
        state = view.findViewById(R.id.state);

        cityInput.setText(store.city());
        updateYes();
        cityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateYes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        cityInput.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO && yes.isEnabled()) {
                load();
                return true;
            }
            return false;
        });

        yes.setOnClickListener(v -> load());
        view.findViewById(R.id.no).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.change).setOnClickListener(v -> showAsk());
    }

    private void updateYes() {
        yes.setEnabled(cityInput.getText().toString().trim().length() >= 2);
    }

    private void load() {
        String city = cityInput.getText().toString().trim();
        if (city.length() < 2) return;
        store.setCity(city);
        hideKeyboard();
        askGroup.setVisibility(View.GONE);
        resultGroup.setVisibility(View.GONE);
        state.showLoading(getString(R.string.briefing_loading));

        if (call != null) call.cancel();
        call = ApiClient.get().briefing(city);
        call.enqueue(new ApiCallback<Briefing>() {
            @Override
            public void onSuccess(@NonNull Briefing body) {
                if (!isAdded()) return;
                showResult(body);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                state.showError(message, BriefingDialogFragment.this::load);
            }
        });
    }

    private void showAsk() {
        state.hide();
        resultGroup.setVisibility(View.GONE);
        askGroup.setVisibility(View.VISIBLE);
    }

    private void showResult(Briefing b) {
        state.hide();
        resultGroup.setVisibility(View.VISIBLE);
        View root = requireView();

        ((TextView) root.findViewById(R.id.cityLabel)).setText(b.city != null ? b.city.toUpperCase(PT_BR) : "");
        TextView temp = root.findViewById(R.id.temp);
        TextView rain = root.findViewById(R.id.rain);
        if (b.weather != null) {
            temp.setVisibility(View.VISIBLE);
            temp.setText(BriefingFormat.temperature(b.weather.temperatureC));
            rain.setText(BriefingFormat.rain(b.weather));
        } else {
            temp.setVisibility(View.GONE);
            rain.setText(b.weatherError != null ? b.weatherError : getString(R.string.briefing_weather_unavailable));
        }
        root.findViewById(R.id.change).setVisibility(b.weather == null ? View.VISIBLE : View.GONE);

        LinearLayout list = root.findViewById(R.id.newsList);
        list.removeAllViews();
        List<Briefing.News> news = b.news != null ? b.news : Collections.<Briefing.News>emptyList();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Briefing.News n : news) {
            View item = inflater.inflate(R.layout.item_briefing_news, list, false);
            ((TextView) item.findViewById(R.id.newsSource)).setText(n.source != null ? n.source.toUpperCase(PT_BR) : "");
            ((TextView) item.findViewById(R.id.newsTitle)).setText(n.title);
            item.setOnClickListener(v -> open(n.url));
            list.addView(item);
        }
        TextView newsError = root.findViewById(R.id.newsError);
        newsError.setVisibility(news.isEmpty() ? View.VISIBLE : View.GONE);
        newsError.setText(b.newsError != null ? b.newsError : getString(R.string.briefing_no_news));
    }

    /** O link vem de um feed externo: só abre http(s). */
    private void open(String url) {
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
            // aparelho sem navegador: nada a fazer
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(cityInput.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        if (call != null) call.cancel();
        super.onDestroyView();
    }
}
