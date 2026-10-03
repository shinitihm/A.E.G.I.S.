package com.example.aegis.ui.jarvis;

import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aegis.MainActivity;
import com.example.aegis.R;
import com.example.aegis.ThemeMode;
import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.LocalStore;
import com.example.aegis.data.model.ChatRequest;
import com.example.aegis.data.model.ChatResponse;
import com.example.aegis.data.model.ChatTurn;
import com.example.aegis.data.model.Health;
import com.example.aegis.ui.arsenal.ArmorDetailActivity;
import com.example.aegis.ui.heroes.CompareActivity;
import com.example.aegis.ui.heroes.HeroDetailActivity;
import com.example.aegis.ui.heroes.HeroesFragment;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.hud.Speaker;
import com.example.aegis.ui.hud.WaveView;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Conversa com a J.A.R.V.I.S.: texto ou voz, resposta falada (TTS) e easter eggs. */
public class JarvisFragment extends Fragment {

    private static final String[] SUGGESTIONS = {
            "Quem é o Thanos?", "Hulk vs Thor", "Mark 44", "Status do arsenal",
            "Qual o alvo mais perigoso?", "House Party", "I am Iron Man"
    };

    private final List<ChatTurn> turns = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView chat;
    private EditText input;
    private TextView mode;
    private WaveView wave;
    private ImageButton voiceToggle;
    private LocalStore store;
    private Speaker speaker;
    private boolean busy;
    private boolean speaking;

    private final ActivityResultLauncher<Intent> speechLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (data == null) return;
                ArrayList<String> heard = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (heard != null && !heard.isEmpty()) ask(heard.get(0));
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle saved) {
        return inflater.inflate(R.layout.fragment_jarvis, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        store = new LocalStore(requireContext());
        speaker = new Speaker(requireContext());
        chat = view.findViewById(R.id.chat);
        input = view.findViewById(R.id.input);
        mode = view.findViewById(R.id.mode);
        wave = view.findViewById(R.id.wave);
        voiceToggle = view.findViewById(R.id.voiceToggle);

        turns.addAll(store.chat());
        if (turns.isEmpty()) turns.add(new ChatTurn(ChatTurn.ASSISTANT, getString(R.string.jarvis_welcome)));
        adapter = new ChatAdapter(turns);
        LinearLayoutManager layout = new LinearLayoutManager(requireContext());
        layout.setStackFromEnd(true);
        chat.setLayoutManager(layout);
        chat.setAdapter(adapter);
        chat.scrollToPosition(turns.size() - 1);

        speaker.setListener(isSpeaking -> {
            speaking = isSpeaking;
            updateWave();
        });

        updateVoiceIcon();
        voiceToggle.setOnClickListener(v -> {
            boolean enabled = !store.voiceEnabled();
            store.setVoiceEnabled(enabled);
            if (!enabled) speaker.stop();
            updateVoiceIcon();
        });
        view.findViewById(R.id.clear).setOnClickListener(v -> clearChat());
        view.findViewById(R.id.send).setOnClickListener(v -> sendFromInput());
        view.findViewById(R.id.mic).setOnClickListener(v -> listen());
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEND) {
                sendFromInput();
                return true;
            }
            return false;
        });

        LinearLayout suggestions = view.findViewById(R.id.suggestions);
        for (String text : SUGGESTIONS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_info, suggestions, false);
            chip.setText(text);
            chip.setClickable(true);
            chip.setOnClickListener(v -> ask(text));
            suggestions.addView(chip);
        }

        loadMode();
    }

    private void loadMode() {
        ApiClient.get().health().enqueue(new ApiCallback<Health>() {
            @Override
            public void onSuccess(@NonNull Health h) {
                if (!isAdded()) return;
                mode.setText("IA".equals(h.jarvis) ? "● MODO IA — CLAUDE" : "● MODO OFFLINE — REGRAS");
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                mode.setText("● SEM CONEXÃO");
            }
        });
    }

    // ── envio ──

    private void sendFromInput() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        input.setText("");
        ask(text);
    }

    /** Usado pelo botão "Perguntar à J.A.R.V.I.S." da ficha do herói. */
    public void ask(String text) {
        if (busy || !isAdded()) return;
        busy = true;
        speaker.stop();

        List<ChatTurn> history = new ArrayList<>(turns);
        turns.add(new ChatTurn(ChatTurn.USER, text));
        adapter.notifyItemInserted(turns.size() - 1);
        chat.scrollToPosition(turns.size() - 1);
        updateWave();

        ApiClient.get().chat(new ChatRequest(text, history)).enqueue(new ApiCallback<ChatResponse>() {
            @Override
            public void onSuccess(@NonNull ChatResponse r) {
                if (!isAdded()) return;
                busy = false;
                reply(r.reply);
                if (r.action != null) handleAction(r.action);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) return;
                busy = false;
                reply(getString(R.string.jarvis_offline_error));
            }
        });
    }

    private void reply(String text) {
        turns.add(new ChatTurn(ChatTurn.ASSISTANT, text));
        adapter.animate(turns.size() - 1);
        adapter.notifyItemInserted(turns.size() - 1);
        chat.scrollToPosition(turns.size() - 1);
        store.saveChat(turns);
        if (store.voiceEnabled()) speaker.speak(text);
        updateWave();
    }

    private void listen() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("pt-BR").toLanguageTag())
                .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.jarvis_voice));
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.no_speech, Toast.LENGTH_LONG).show();
        }
    }

    private void clearChat() {
        speaker.stop();
        turns.clear();
        turns.add(new ChatTurn(ChatTurn.ASSISTANT, getString(R.string.jarvis_welcome)));
        adapter.notifyDataSetChanged();
        store.saveChat(turns);
    }

    // ── ações vindas do backend ──

    private void handleAction(String action) {
        MainActivity main = (MainActivity) requireActivity();
        if ("HOUSE_PARTY".equals(action)) {
            main.showTab(R.id.nav_arsenal);
            main.arsenal().startHouseParty();
        } else if ("IRON_MAN".equals(action)) {
            Hud.flash(main.flashOverlay(), 0x66FFB300);
            Hud.vibrate(main, 200);
        } else if ("CLEAN_SLATE".equals(action)) {
            Hud.flash(main.flashOverlay(), 0x99FFFFFF);
            Hud.vibrate(main, 500);
        } else if ("SNAP".equals(action)) {
            Hud.flash(main.flashOverlay(), 0x66B388FF);
            Hud.vibrate(main, 300);
            // espera a resposta ser falada/lida: trocar de aba esconde o chat e corta a voz
            chat.postDelayed(() -> {
                if (!isResumed()) return; // app foi para segundo plano: não troca de aba
                main.showTab(R.id.nav_targets);
                main.fragment("targets", HeroesFragment.class).snap();
            }, store.voiceEnabled() ? 3500 : 1500);
        } else if ("ULTRON".equals(action)) {
            glitch(main);
        } else if ("DOOM_MODE".equals(action)) {
            switchTheme(main, true);
        } else if ("STARK_MODE".equals(action)) {
            switchTheme(main, false);
        } else if (action.startsWith("OPEN_ARMOR:")) {
            startActivity(ArmorDetailActivity.intent(requireContext(), parseInt(action)));
        } else if (action.startsWith("OPEN_HERO:")) {
            startActivity(HeroDetailActivity.intent(requireContext(), parseInt(action)));
        } else if (action.startsWith("COMPARE:")) {
            String[] names = action.substring("COMPARE:".length()).split("\\|");
            if (names.length == 2) startActivity(CompareActivity.intent(requireContext(), names[0], names[1]));
        }
    }

    /** Easter egg "Ultron": a tela treme em vermelho (invasão) e depois pisca em ciano (J.A.R.V.I.S. retoma). */
    private void glitch(MainActivity main) {
        View content = main.findViewById(android.R.id.content);
        Hud.flash(main.flashOverlay(), 0x99FF1744);
        Hud.vibrate(main, 600);
        ObjectAnimator shake = ObjectAnimator.ofFloat(content, View.TRANSLATION_X, 0, -28, 22, -16, 30, -10, 14, 0);
        shake.setDuration(400);
        shake.setRepeatCount(2);
        shake.start();
        content.postDelayed(() -> Hud.flash(main.flashOverlay(), 0x6600E5FF), 1400);
    }

    /**
     * Troca o tema depois de a J.A.R.V.I.S. responder. A troca recria as telas; o chat não se perde porque
     * reply() já o salvou no LocalStore antes desta ação.
     */
    private void switchTheme(MainActivity main, boolean doom) {
        if (store.doomMode() == doom) return; // já está nesse tema: não recria as telas à toa
        store.setDoomMode(doom);
        Hud.flash(main.flashOverlay(), doom ? ThemeMode.FLASH_DOOM : ThemeMode.FLASH_STARK);
        Hud.vibrate(main, 250);
        long wait = store.voiceEnabled() ? 3500 : 1500; // deixa a resposta ser falada/lida antes de recriar
        // Handler do Looper principal, não da view: a troca acontece mesmo se o usuário sair da tela nesse intervalo
        new Handler(Looper.getMainLooper()).postDelayed(() -> ThemeMode.apply(doom), wait);
    }

    private static int parseInt(String action) {
        try {
            return Integer.parseInt(action.substring(action.indexOf(':') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ── UI ──

    private void updateWave() {
        wave.setVisibility(busy || speaking ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateVoiceIcon() {
        boolean on = store.voiceEnabled();
        voiceToggle.setImageResource(on ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
        voiceToggle.setColorFilter(requireContext().getColor(on ? R.color.aegis_cyan : R.color.aegis_text_dim));
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // pode chegar antes do onViewCreated (a MainActivity adiciona e esconde as abas no onCreate): speaker ainda é null
        if (hidden && speaker != null) speaker.stop(); // não continua falando depois que o usuário sai da aba
    }

    @Override
    public void onDestroyView() {
        speaker.shutdown();
        super.onDestroyView();
    }
}
